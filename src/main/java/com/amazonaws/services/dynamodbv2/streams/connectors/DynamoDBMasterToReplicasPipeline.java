/*
 * Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.dynamodbv2.streams.connectors;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.kinesis.connectors.KinesisConnectorConfiguration;
import com.amazonaws.services.kinesis.connectors.impl.AllPassFilter;
import com.amazonaws.services.kinesis.connectors.interfaces.IBuffer;
import com.amazonaws.services.kinesis.connectors.interfaces.IEmitter;
import com.amazonaws.services.kinesis.connectors.interfaces.IFilter;
import com.amazonaws.services.kinesis.connectors.interfaces.IKinesisConnectorPipeline;
import com.amazonaws.services.kinesis.connectors.interfaces.ITransformer;

import java.util.concurrent.Executors;

import static com.amazonaws.services.dynamodbv2.streams.connectors.DynamoDBReplicationEmitter.MAX_THREADS;

/**
 * The Pipeline used when there is only one single master replicating to multiple replicas. Uses:
 * <ul>
 * <li>{@link DynamoDBReplicationEmitter}</li>
 * <li>{@link DynamoDBBuffer}</li>
 * <li>{@link DynamoDBStreamsRecordTransformer}</li>
 * <li>{@link AllPassFilter}</li>
 * </ul>
 */

public class DynamoDBMasterToReplicasPipeline implements IKinesisConnectorPipeline<Record, Record> {

    @Override
    public IEmitter<Record> getEmitter(final KinesisConnectorConfiguration configuration) {
        if (configuration instanceof DynamoDBStreamsConnectorConfiguration) {
            AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
            ClientConfiguration clientConfiguration = new ClientConfiguration()
                    .withMaxConnections(MAX_THREADS)
                    .withRetryPolicy(PredefinedRetryPolicies.DYNAMODB_DEFAULT);

            AmazonDynamoDBAsync client = AmazonDynamoDBAsyncClient.asyncBuilder()
                    .withExecutorFactory(() -> Executors.newFixedThreadPool(MAX_THREADS))
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(
                                    configuration.DYNAMODB_ENDPOINT,
                                    configuration.REGION_NAME
                            )
                    )
                    .withCredentials(credentialsProvider)
                    .withClientConfiguration(clientConfiguration)
                    .build();

            AmazonCloudWatchAsync cloudWatch = AmazonCloudWatchAsyncClient.asyncBuilder()
                    .withCredentials(credentialsProvider)
                    .withExecutorFactory(() -> Executors.newFixedThreadPool(MAX_THREADS))
                    .withRegion(Regions.US_EAST_1)
                    .build();
            return new DynamoDBReplicationEmitter(
                    configuration.APP_NAME,
                    configuration.DYNAMODB_ENDPOINT,
                    configuration.REGION_NAME,
                    configuration.DYNAMODB_DATA_TABLE_NAME,
                    client,
                    cloudWatch);
        } else {
            throw new IllegalArgumentException(this + " needs a DynamoDBStreamsConnectorConfiguration argument.");
        }

    }

    @Override
    public IBuffer<Record> getBuffer(final KinesisConnectorConfiguration configuration) {
        if (configuration instanceof DynamoDBStreamsConnectorConfiguration) {
            return new DynamoDBBuffer((DynamoDBStreamsConnectorConfiguration) configuration);
        } else {
            throw new IllegalArgumentException(this + " needs a DynamoDBStreamsConnectorConfiguration argument.");
        }
    }

    @Override
    public ITransformer<Record, Record> getTransformer(final KinesisConnectorConfiguration configuration) {
        return new DynamoDBStreamsRecordTransformer();
    }

    @Override
    public IFilter<Record> getFilter(final KinesisConnectorConfiguration configuration) {
        return new AllPassFilter<Record>();
    }

}
