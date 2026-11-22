package ec.uce.pd.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class StudentClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentClientApplication.class, args);
    }

    @Bean
    RestClient enrollmentClient(@Value("${enrollment.url}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(3000);
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
}
