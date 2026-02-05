# S3StorageClient - AWSCredentialsProvider 빈 누락

## 에러

```
Parameter 1 of constructor in com.tasteam.infra.storage.s3.S3StorageClient
required a bean of type 'com.amazonaws.auth.AWSCredentialsProvider' that could not be found.
```

배포 시 `STORAGE_TYPE=s3` 환경변수로 S3 모드가 활성화되었으나 애플리케이션 기동 실패.

## 원인

`S3StorageClient`는 생성자 주입으로 `AWSCredentialsProvider`를 요구하지만,
`S3StorageConfig`에는 `AmazonS3` 빈만 정의되어 있고 `AWSCredentialsProvider` 빈이 없었다.

```java
// S3StorageClient 생성자 (Lombok @RequiredArgsConstructor)
private final AmazonS3 amazonS3;
private final AWSCredentialsProvider credentialsProvider; // ← 빈 없음
private final StorageProperties properties;
```

## 해결

`S3StorageConfig`에 `AWSCredentialsProvider` 빈을 추가하고, `AmazonS3` 빈이 이를 주입받도록 변경.

```java
@Bean
public AWSCredentialsProvider awsCredentialsProvider(StorageProperties properties) {
    if (properties.hasStaticCredentials()) {
        return new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey()));
    }
    return DefaultAWSCredentialsProviderChain.getInstance();
}

@Bean
public AmazonS3 amazonS3(StorageProperties properties, AWSCredentialsProvider credentialsProvider) {
    return AmazonS3ClientBuilder.standard()
        .withRegion(properties.getRegion())
        .withCredentials(credentialsProvider)
        .build();
}
```

추가로 `application.dev.yml`, `application.prod.yml`에 `tasteam.storage.type: s3`를 명시하여
환경변수 없이도 S3 모드가 활성화되도록 수정.
