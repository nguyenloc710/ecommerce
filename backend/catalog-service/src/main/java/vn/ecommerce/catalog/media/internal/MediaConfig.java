package vn.ecommerce.catalog.media.internal;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(MediaProperties.class)
class MediaConfig {

    /** Ký URL hoàn toàn cục bộ (không gọi mạng), nên không ảnh hưởng thời gian phản hồi API. */
    @Bean(destroyMethod = "close")
    S3Presigner mediaPresigner(MediaProperties props) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of(props.region()))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(props.pathStyle()).build())
                .build();
    }

    /** Chỉ dùng để tạo bucket ở dev (createBucketOnStartup); ảnh không đi qua service. */
    @Bean(destroyMethod = "close")
    S3Client mediaS3Client(MediaProperties props) {
        return S3Client.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of(props.region()))
                .credentialsProvider(credentials(props))
                .forcePathStyle(props.pathStyle())
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    private static StaticCredentialsProvider credentials(MediaProperties props) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
    }
}
