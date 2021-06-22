package com.amazonaws.services.dynamodbv2.streams.connectors;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder;
import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.dynamodbv2.streams.connectors.composite.CompositeRecordProcessorFactory;
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.connectors.KinesisConnectorRecordProcessorFactory;
import com.amazonaws.services.kinesis.connectors.interfaces.IKinesisConnectorPipeline;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

public class KinesisWorkerCreator {
    private Region sourceRegion;
    private Optional<String> sourceDynamodbEndpoint = Optional.empty();
    private Optional<String> sourceDynamodbAccessKeyId = Optional.empty();
    private Optional<String> sourceDynamodbSecretAccessKey = Optional.empty();
    private Optional<String> sourceDynamodbStreamsEndpoint = Optional.empty();
    private String sourceTable;
    private Optional<Region> kclRegion = Optional.empty();
    private Optional<String> kclDynamodbEndpoint = Optional.empty();
    private Region destinationRegion;
    private Optional<String> destinationDynamodbEndpoint = Optional.empty();
    private Optional<String> destinationDynamodbAccessKeyId = Optional.empty();
    private Optional<String> destinationDynamodbSecretAccessKey = Optional.empty();
    private Optional<Integer> getRecordsLimit = Optional.empty();
    private boolean isPublishCloudWatch;
    private String taskName;
    private String partitionKeyName;
    private String lastUpdateTimeKeyName;
    private String destinationTable;
    private List<IKinesisConnectorPipeline<Record, Record>> pipelines = new ArrayList<>();
    private Optional<Long> parentShardPollIntervalMillis = Optional.empty();
    private long failoverTimeMillis = DynamoDBConnectorConstants.KCL_FAILOVER_TIME;

    public KinesisWorkerCreator() {
    }

    public KinesisWorkerCreator(CommandLineArgs params) {
        // extract streams endpoint, source and destination regions
        sourceRegion = RegionUtils.getRegion(params.getSourceSigningRegion());

        // set the source dynamodb endpoint
        sourceDynamodbEndpoint = Optional.ofNullable(params.getSourceEndpoint());
        sourceDynamodbStreamsEndpoint = Optional.ofNullable(params.getSourceEndpoint());

        // get source table name
        sourceTable = params.getSourceTable();

        // get kcl endpoint and region or null for region if cannot parse region from endpoint
        kclRegion = Optional.ofNullable(RegionUtils.getRegion(params.getKclSigningRegion()));
        kclDynamodbEndpoint = Optional.ofNullable(params.getKclEndpoint());

        // get destination endpoint and region or null for region if cannot parse region from endpoint
        destinationRegion = RegionUtils.getRegion(params.getDestinationSigningRegion());
        destinationDynamodbEndpoint = Optional.ofNullable(params.getDestinationEndpoint());
        destinationTable = params.getDestinationTable();

        // other crr parameters
        getRecordsLimit = Optional.ofNullable(params.getBatchSize());
        isPublishCloudWatch = !params.isDontPublishCloudwatch();
        taskName = params.getTaskName();
        parentShardPollIntervalMillis = Optional.ofNullable(params.getParentShardPollIntervalMillis());

        pipelines.add(new DynamoDBMasterToReplicasPipeline());
    }

    public Worker create() {
        // use default credential provider chain to locate appropriate credentials
        final AWSCredentialsProvider sourceCredentialsProvider = createSourceRegionCredentialProvider();
        final AWSCredentialsProvider destinationCredentialsProvider = createDestinationRegionCredentialProvider();

        // initialize DynamoDB client and set the endpoint properly for source table / region
        final AmazonDynamoDB dynamodbClient = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(sourceCredentialsProvider)
                .withEndpointConfiguration(createEndpointConfiguration(sourceRegion, sourceDynamodbEndpoint, AmazonDynamoDB.ENDPOINT_PREFIX))
                .build();

        // initialize Streams client
        final AwsClientBuilder.EndpointConfiguration streamsEndpointConfiguration = createEndpointConfiguration(sourceRegion,
                sourceDynamodbStreamsEndpoint, AmazonDynamoDBStreams.ENDPOINT_PREFIX);
        final ClientConfiguration streamsClientConfig = new ClientConfiguration().withGzip(false);
        final AmazonDynamoDBStreams streamsClient = AmazonDynamoDBStreamsClientBuilder.standard()
                .withCredentials(sourceCredentialsProvider)
                .withEndpointConfiguration(streamsEndpointConfiguration)
                .withClientConfiguration(streamsClientConfig)
                .build();

        // obtain the Stream ID associated with the source table
        final String streamArn = dynamodbClient.describeTable(sourceTable).getTable().getLatestStreamArn();
        final boolean streamEnabled = DynamoDBConnectorUtilities.isStreamsEnabled(streamsClient, streamArn, DynamoDBConnectorConstants.NEW_AND_OLD);
        Preconditions.checkArgument(streamArn != null, DynamoDBConnectorConstants.MSG_NO_STREAMS_FOUND);
        Preconditions.checkState(streamEnabled, DynamoDBConnectorConstants.STREAM_NOT_READY);

        // initialize DynamoDB client for KCL
        final AmazonDynamoDB kclDynamoDBClient = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(sourceCredentialsProvider)
                .withEndpointConfiguration(createKclDynamoDbEndpointConfiguration())
                .build();

        // initialize DynamoDB Streams Adapter client and set the Streams endpoint properly
        final AmazonDynamoDBStreamsAdapterClient streamsAdapterClient = new AmazonDynamoDBStreamsAdapterClient(streamsClient);

        // initialize CloudWatch client and set the region to emit metrics to
        final AmazonCloudWatch kclCloudWatchClient;
        if (isPublishCloudWatch) {
            kclCloudWatchClient = AmazonCloudWatchClientBuilder.standard()
                    .withCredentials(sourceCredentialsProvider)
                    .withRegion(kclRegion.orElse(sourceRegion).getName()).build();
        } else {
            kclCloudWatchClient = new NoopCloudWatch();
        }

        // try to get taskname from command line arguments, auto generate one if needed
        final AwsClientBuilder.EndpointConfiguration destinationEndpointConfiguration = createEndpointConfiguration(destinationRegion,
                destinationDynamodbEndpoint, AmazonDynamoDB.ENDPOINT_PREFIX);
        final String actualTaskName = DynamoDBConnectorUtilities.getTaskName(sourceRegion, destinationRegion, taskName, sourceTable, destinationTable);

        // set the appropriate Connector properties for the destination KCL configuration
        final Properties properties = new Properties();
        properties.put(DynamoDBStreamsConnectorConfiguration.PROP_APP_NAME, actualTaskName);
        properties.put(DynamoDBStreamsConnectorConfiguration.PROP_DYNAMODB_ENDPOINT, destinationEndpointConfiguration.getServiceEndpoint());
        properties.put(DynamoDBStreamsConnectorConfiguration.PROP_DYNAMODB_DATA_TABLE_NAME, destinationTable);
        properties.put(DynamoDBStreamsConnectorConfiguration.PROP_REGION_NAME, destinationRegion.getName());

        // create the record processor factory based on given pipeline and connector configurations
        // use the master to replicas pipeline
        final List<KinesisConnectorRecordProcessorFactory<Record, Record>> factories = pipelines.stream().map(pipeline ->
                new KinesisConnectorRecordProcessorFactory<>(
                        pipeline,
                        new DynamoDBStreamsConnectorConfiguration(
                                properties, destinationCredentialsProvider, isPublishCloudWatch, partitionKeyName, lastUpdateTimeKeyName))
        ).collect(Collectors.toList());

        // create the KCL configuration with default values
        final KinesisClientLibConfiguration kclConfig = new KinesisClientLibConfiguration(actualTaskName,
                streamArn,
                sourceCredentialsProvider,
                DynamoDBConnectorConstants.WORKER_LABEL + actualTaskName + UUID.randomUUID().toString())
                // worker will use checkpoint table if available, otherwise it is safer
                // to start at beginning of the stream
                .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)
                // we want the maximum batch size to avoid network transfer latency overhead
                .withMaxRecords(getRecordsLimit.orElse(DynamoDBConnectorConstants.STREAMS_RECORDS_LIMIT))
                // wait a reasonable amount of time - default 0.5 seconds
                .withIdleTimeBetweenReadsInMillis(DynamoDBConnectorConstants.IDLE_TIME_BETWEEN_READS)
                // Remove calls to GetShardIterator
                .withValidateSequenceNumberBeforeCheckpointing(false)
                // make parent shard poll interval tunable to decrease time to run integration test
                .withParentShardPollIntervalMillis(parentShardPollIntervalMillis.orElse(DynamoDBConnectorConstants.DEFAULT_PARENT_SHARD_POLL_INTERVAL_MILLIS))
                // avoid losing leases too often - default 60 seconds
                .withFailoverTimeMillis(failoverTimeMillis);

        // create the KCL worker for this connector
        return new Worker.Builder()
                .recordProcessorFactory(new CompositeRecordProcessorFactory(factories))
                .config(kclConfig)
                .kinesisClient(streamsAdapterClient)
                .dynamoDBClient(kclDynamoDBClient)
                .cloudWatchClient(kclCloudWatchClient)
                .build();
    }

    private AWSCredentialsProvider createSourceRegionCredentialProvider() {
        if (sourceDynamodbAccessKeyId.isPresent()) {
            return new ConstantAwsCredentialsProvider(sourceDynamodbAccessKeyId.get(), sourceDynamodbSecretAccessKey.get());
        } else {
            return new DefaultAWSCredentialsProviderChain();
        }
    }

    private AWSCredentialsProvider createDestinationRegionCredentialProvider() {
        if (destinationDynamodbAccessKeyId.isPresent()) {
            return new ConstantAwsCredentialsProvider(destinationDynamodbAccessKeyId.get(), destinationDynamodbSecretAccessKey.get());
        } else {
            return new DefaultAWSCredentialsProviderChain();
        }
    }

    private static class ConstantAwsCredentialsProvider implements AWSCredentialsProvider {
        private final String accessKeyId;
        private final String secretAccessKey;

        public ConstantAwsCredentialsProvider(String accessKeyId, String secretAccessKey) {
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
        }

        @Override
        public AWSCredentials getCredentials() {
            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return accessKeyId;
                }

                @Override
                public String getAWSSecretKey() {
                    return secretAccessKey;
                }
            };
        }

        @Override
        public void refresh() {
        }
    }

    @VisibleForTesting
    static AwsClientBuilder.EndpointConfiguration createEndpointConfiguration(Region region, Optional<String> endpoint, String endpointPrefix) {
        return new AwsClientBuilder.EndpointConfiguration(endpoint.orElse("https://" + region.getServiceEndpoint(endpointPrefix)), region.getName());
    }


    @VisibleForTesting
    AwsClientBuilder.EndpointConfiguration createKclDynamoDbEndpointConfiguration() {
        return createEndpointConfiguration(kclRegion.orElse(sourceRegion),
                kclRegion.isPresent() ? kclDynamodbEndpoint : sourceDynamodbEndpoint, AmazonDynamoDB.ENDPOINT_PREFIX);
    }

    public Region getSourceRegion() {
        return sourceRegion;
    }

    public KinesisWorkerCreator setSourceRegion(Region sourceRegion) {
        this.sourceRegion = sourceRegion;
        return this;
    }

    public Optional<String> getSourceDynamodbEndpoint() {
        return sourceDynamodbEndpoint;
    }

    public KinesisWorkerCreator setSourceDynamodbEndpoint(String sourceDynamodbEndpoint) {
        this.sourceDynamodbEndpoint = Optional.ofNullable(sourceDynamodbEndpoint);
        return this;
    }

    public Optional<String> getSourceDynamodbStreamsEndpoint() {
        return sourceDynamodbStreamsEndpoint;
    }

    public KinesisWorkerCreator setSourceDynamodbStreamsEndpoint(String sourceDynamodbStreamsEndpoint) {
        this.sourceDynamodbStreamsEndpoint = Optional.ofNullable(sourceDynamodbStreamsEndpoint);
        return this;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public KinesisWorkerCreator setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
        return this;
    }

    public Optional<Region> getKclRegion() {
        return kclRegion;
    }

    public KinesisWorkerCreator setKclRegion(Region kclRegion) {
        this.kclRegion = Optional.ofNullable(kclRegion);
        return this;
    }

    public Optional<String> getKclDynamodbEndpoint() {
        return kclDynamodbEndpoint;
    }

    public KinesisWorkerCreator setKclDynamodbEndpoint(String kclDynamodbEndpoint) {
        this.kclDynamodbEndpoint = Optional.ofNullable(kclDynamodbEndpoint);
        return this;
    }

    public Region getDestinationRegion() {
        return destinationRegion;
    }

    public KinesisWorkerCreator setDestinationRegion(Region destinationRegion) {
        this.destinationRegion = destinationRegion;
        return this;
    }

    public Optional<String> getDestinationDynamodbEndpoint() {
        return destinationDynamodbEndpoint;
    }

    public KinesisWorkerCreator setDestinationDynamodbEndpoint(String destinationDynamodbEndpoint) {
        this.destinationDynamodbEndpoint = Optional.ofNullable(destinationDynamodbEndpoint);
        return this;
    }

    public Optional<Integer> getGetRecordsLimit() {
        return getRecordsLimit;
    }

    public KinesisWorkerCreator setGetRecordsLimit(Integer getRecordsLimit) {
        this.getRecordsLimit = Optional.ofNullable(getRecordsLimit);
        return this;
    }

    public boolean isPublishCloudWatch() {
        return isPublishCloudWatch;
    }

    public KinesisWorkerCreator setPublishCloudWatch(boolean publishCloudWatch) {
        isPublishCloudWatch = publishCloudWatch;
        return this;
    }

    public String getTaskName() {
        return taskName;
    }

    public KinesisWorkerCreator setTaskName(String taskName) {
        this.taskName = taskName;
        return this;
    }

    public String getDestinationTable() {
        return destinationTable;
    }

    public KinesisWorkerCreator setDestinationTable(String destinationTable) {
        this.destinationTable = destinationTable;
        return this;
    }

    public List<IKinesisConnectorPipeline<Record, Record>> getPipelines() {
        return pipelines;
    }

    public KinesisWorkerCreator addPipeline(IKinesisConnectorPipeline<Record, Record> pipeline) {
        this.pipelines.add(pipeline);
        return this;
    }

    public Optional<Long> getParentShardPollIntervalMillis() {
        return parentShardPollIntervalMillis;
    }

    public KinesisWorkerCreator setParentShardPollIntervalMillis(Long parentShardPollIntervalMillis) {
        this.parentShardPollIntervalMillis = Optional.ofNullable(parentShardPollIntervalMillis);
        return this;
    }

    public KinesisWorkerCreator setPartitionKeyName(String partitionKeyName) {
        this.partitionKeyName = partitionKeyName;
        return this;
    }

    public KinesisWorkerCreator setLastUpdateTimeKeyName(String lastUpdateTimeKeyName) {
        this.lastUpdateTimeKeyName = lastUpdateTimeKeyName;
        return this;
    }

    public KinesisWorkerCreator setFailoverTimeMillis(long failoverTimeMillis) {
        this.failoverTimeMillis = failoverTimeMillis;
        return this;
    }

    public KinesisWorkerCreator setSourceDynamodbCredentials(String accessKeyId, String secretAccessKey) {
        this.sourceDynamodbAccessKeyId = Optional.of(accessKeyId);
        this.sourceDynamodbSecretAccessKey = Optional.of(secretAccessKey);
        return this;
    }

    public KinesisWorkerCreator setDestinationDynamodbCredentials(String accessKeyId, String secretAccessKey) {
        this.destinationDynamodbAccessKeyId = Optional.of(accessKeyId);
        this.destinationDynamodbSecretAccessKey = Optional.of(secretAccessKey);
        return this;
    }
}
