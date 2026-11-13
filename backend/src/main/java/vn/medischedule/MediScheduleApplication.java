package vn.medischedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@ConfigurationPropertiesScan
// Page trả về dạng ổn định: { content: [...], page: { size, number, totalElements, totalPages } }
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class MediScheduleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediScheduleApplication.class, args);
    }
}
