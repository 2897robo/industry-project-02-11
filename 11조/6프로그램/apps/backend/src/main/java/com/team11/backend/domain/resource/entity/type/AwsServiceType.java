package com.team11.backend.domain.resource.entity.type;

public enum AwsServiceType {
    EC2("Amazon EC2"),
    RDS("Amazon RDS"),
    S3("Amazon S3"),
    Lambda("AWS Lambda"),
    EBS("Amazon EBS"),
    CloudFront("Amazon CloudFront"),
    DynamoDB("Amazon DynamoDB"),
    ElastiCache("Amazon ElastiCache"),
    ELB("Elastic Load Balancing"),
    Route53("Amazon Route 53");

    private final String displayName;

    AwsServiceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
