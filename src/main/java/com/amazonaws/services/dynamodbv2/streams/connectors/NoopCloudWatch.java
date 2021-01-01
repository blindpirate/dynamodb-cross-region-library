package com.amazonaws.services.dynamodbv2.streams.connectors;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsResult;
import com.amazonaws.services.cloudwatch.model.DeleteAnomalyDetectorRequest;
import com.amazonaws.services.cloudwatch.model.DeleteAnomalyDetectorResult;
import com.amazonaws.services.cloudwatch.model.DeleteDashboardsRequest;
import com.amazonaws.services.cloudwatch.model.DeleteDashboardsResult;
import com.amazonaws.services.cloudwatch.model.DeleteInsightRulesRequest;
import com.amazonaws.services.cloudwatch.model.DeleteInsightRulesResult;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmHistoryRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmHistoryResult;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsForMetricRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsForMetricResult;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.DescribeAnomalyDetectorsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAnomalyDetectorsResult;
import com.amazonaws.services.cloudwatch.model.DescribeInsightRulesRequest;
import com.amazonaws.services.cloudwatch.model.DescribeInsightRulesResult;
import com.amazonaws.services.cloudwatch.model.DisableAlarmActionsRequest;
import com.amazonaws.services.cloudwatch.model.DisableAlarmActionsResult;
import com.amazonaws.services.cloudwatch.model.DisableInsightRulesRequest;
import com.amazonaws.services.cloudwatch.model.DisableInsightRulesResult;
import com.amazonaws.services.cloudwatch.model.EnableAlarmActionsRequest;
import com.amazonaws.services.cloudwatch.model.EnableAlarmActionsResult;
import com.amazonaws.services.cloudwatch.model.EnableInsightRulesRequest;
import com.amazonaws.services.cloudwatch.model.EnableInsightRulesResult;
import com.amazonaws.services.cloudwatch.model.GetDashboardRequest;
import com.amazonaws.services.cloudwatch.model.GetDashboardResult;
import com.amazonaws.services.cloudwatch.model.GetInsightRuleReportRequest;
import com.amazonaws.services.cloudwatch.model.GetInsightRuleReportResult;
import com.amazonaws.services.cloudwatch.model.GetMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricDataResult;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.GetMetricWidgetImageRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricWidgetImageResult;
import com.amazonaws.services.cloudwatch.model.ListDashboardsRequest;
import com.amazonaws.services.cloudwatch.model.ListDashboardsResult;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.ListTagsForResourceRequest;
import com.amazonaws.services.cloudwatch.model.ListTagsForResourceResult;
import com.amazonaws.services.cloudwatch.model.PutAnomalyDetectorRequest;
import com.amazonaws.services.cloudwatch.model.PutAnomalyDetectorResult;
import com.amazonaws.services.cloudwatch.model.PutCompositeAlarmRequest;
import com.amazonaws.services.cloudwatch.model.PutCompositeAlarmResult;
import com.amazonaws.services.cloudwatch.model.PutDashboardRequest;
import com.amazonaws.services.cloudwatch.model.PutDashboardResult;
import com.amazonaws.services.cloudwatch.model.PutInsightRuleRequest;
import com.amazonaws.services.cloudwatch.model.PutInsightRuleResult;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmResult;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.amazonaws.services.cloudwatch.model.SetAlarmStateRequest;
import com.amazonaws.services.cloudwatch.model.SetAlarmStateResult;
import com.amazonaws.services.cloudwatch.model.TagResourceRequest;
import com.amazonaws.services.cloudwatch.model.TagResourceResult;
import com.amazonaws.services.cloudwatch.model.UntagResourceRequest;
import com.amazonaws.services.cloudwatch.model.UntagResourceResult;
import com.amazonaws.services.cloudwatch.waiters.AmazonCloudWatchWaiters;

/**
 * This class allows KCL to be started in a local environment against DynamoDB Local,
 * without CloudWatch connectivity.
 */
public class NoopCloudWatch implements AmazonCloudWatch {
    @Override
    public void setEndpoint(String s) {

    }

    @Override
    public void setRegion(Region region) {

    }

    @Override
    public DeleteAlarmsResult deleteAlarms(DeleteAlarmsRequest deleteAlarmsRequest) {
        return null;
    }

    @Override
    public DeleteAnomalyDetectorResult deleteAnomalyDetector(DeleteAnomalyDetectorRequest deleteAnomalyDetectorRequest) {
        return null;
    }

    @Override
    public DeleteDashboardsResult deleteDashboards(DeleteDashboardsRequest deleteDashboardsRequest) {
        return null;
    }

    @Override
    public DeleteInsightRulesResult deleteInsightRules(DeleteInsightRulesRequest deleteInsightRulesRequest) {
        return null;
    }

    @Override
    public DescribeAlarmHistoryResult describeAlarmHistory(DescribeAlarmHistoryRequest describeAlarmHistoryRequest) {
        return null;
    }

    @Override
    public DescribeAlarmHistoryResult describeAlarmHistory() {
        return null;
    }

    @Override
    public DescribeAlarmsResult describeAlarms(DescribeAlarmsRequest describeAlarmsRequest) {
        return null;
    }

    @Override
    public DescribeAlarmsResult describeAlarms() {
        return null;
    }

    @Override
    public DescribeAlarmsForMetricResult describeAlarmsForMetric(DescribeAlarmsForMetricRequest describeAlarmsForMetricRequest) {
        return null;
    }

    @Override
    public DescribeAnomalyDetectorsResult describeAnomalyDetectors(DescribeAnomalyDetectorsRequest describeAnomalyDetectorsRequest) {
        return null;
    }

    @Override
    public DescribeInsightRulesResult describeInsightRules(DescribeInsightRulesRequest describeInsightRulesRequest) {
        return null;
    }

    @Override
    public DisableAlarmActionsResult disableAlarmActions(DisableAlarmActionsRequest disableAlarmActionsRequest) {
        return null;
    }

    @Override
    public DisableInsightRulesResult disableInsightRules(DisableInsightRulesRequest disableInsightRulesRequest) {
        return null;
    }

    @Override
    public EnableAlarmActionsResult enableAlarmActions(EnableAlarmActionsRequest enableAlarmActionsRequest) {
        return null;
    }

    @Override
    public EnableInsightRulesResult enableInsightRules(EnableInsightRulesRequest enableInsightRulesRequest) {
        return null;
    }

    @Override
    public GetDashboardResult getDashboard(GetDashboardRequest getDashboardRequest) {
        return null;
    }

    @Override
    public GetInsightRuleReportResult getInsightRuleReport(GetInsightRuleReportRequest getInsightRuleReportRequest) {
        return null;
    }

    @Override
    public GetMetricDataResult getMetricData(GetMetricDataRequest getMetricDataRequest) {
        return null;
    }

    @Override
    public GetMetricStatisticsResult getMetricStatistics(GetMetricStatisticsRequest getMetricStatisticsRequest) {
        return null;
    }

    @Override
    public GetMetricWidgetImageResult getMetricWidgetImage(GetMetricWidgetImageRequest getMetricWidgetImageRequest) {
        return null;
    }

    @Override
    public ListDashboardsResult listDashboards(ListDashboardsRequest listDashboardsRequest) {
        return null;
    }

    @Override
    public ListMetricsResult listMetrics(ListMetricsRequest listMetricsRequest) {
        return null;
    }

    @Override
    public ListMetricsResult listMetrics() {
        return null;
    }

    @Override
    public ListTagsForResourceResult listTagsForResource(ListTagsForResourceRequest listTagsForResourceRequest) {
        return null;
    }

    @Override
    public PutAnomalyDetectorResult putAnomalyDetector(PutAnomalyDetectorRequest putAnomalyDetectorRequest) {
        return null;
    }

    @Override
    public PutCompositeAlarmResult putCompositeAlarm(PutCompositeAlarmRequest putCompositeAlarmRequest) {
        return null;
    }

    @Override
    public PutDashboardResult putDashboard(PutDashboardRequest putDashboardRequest) {
        return null;
    }

    @Override
    public PutInsightRuleResult putInsightRule(PutInsightRuleRequest putInsightRuleRequest) {
        return null;
    }

    @Override
    public PutMetricAlarmResult putMetricAlarm(PutMetricAlarmRequest putMetricAlarmRequest) {
        return null;
    }

    @Override
    public PutMetricDataResult putMetricData(PutMetricDataRequest putMetricDataRequest) {
        return null;
    }

    @Override
    public SetAlarmStateResult setAlarmState(SetAlarmStateRequest setAlarmStateRequest) {
        return null;
    }

    @Override
    public TagResourceResult tagResource(TagResourceRequest tagResourceRequest) {
        return null;
    }

    @Override
    public UntagResourceResult untagResource(UntagResourceRequest untagResourceRequest) {
        return null;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) {
        return null;
    }

    @Override
    public AmazonCloudWatchWaiters waiters() {
        return null;
    }
}
