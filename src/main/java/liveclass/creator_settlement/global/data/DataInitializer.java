package liveclass.creator_settlement.global.data;

import liveclass.creator_settlement.domain.cancelRecord.CancelRecord;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.course.Course;
import liveclass.creator_settlement.domain.course.CourseRepository;
import liveclass.creator_settlement.domain.creator.Creator;
import liveclass.creator_settlement.domain.creator.CreatorRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.constant.SaleRecordStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CreatorRepository creatorRepository;
    private final CourseRepository courseRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        ClassPathResource resource = new ClassPathResource("data/sample-data.json");
        JsonNode root = objectMapper.readTree(resource.getInputStream());

        for (JsonNode node : root.get("creators")) {
            String id = node.get("id").asString();
            if (!creatorRepository.existsById(id)) {
                creatorRepository.save(Creator.of(id, node.get("name").asString()));
            }
        }

        for (JsonNode node : root.get("courses")) {
            String id = node.get("id").asString();
            if (!courseRepository.existsById(id)) {
                courseRepository.save(Course.of(
                    id,
                    node.get("creatorId").asString(),
                    node.get("title").asString()
                ));
            }
        }

        for (JsonNode node : root.get("saleRecords")) {
            String id = node.get("id").asString();
            if (!saleRecordRepository.existsById(id)) {
                saleRecordRepository.save(SaleRecord.of(
                    id,
                    node.get("courseId").asString(),
                    node.get("studentId").asString(),
                    node.get("amount").decimalValue(),
                    LocalDateTime.parse(node.get("paidAt").asString())
                ));
            }
        }

        for (JsonNode node : root.get("cancelRecords")) {
            String id = node.get("id").asString();
            if (!cancelRecordRepository.existsById(id)) {
                String saleRecordId = node.get("saleRecordId").asString();
                cancelRecordRepository.save(CancelRecord.of(
                    id,
                    saleRecordId,
                    BigDecimal.valueOf(node.get("refundAmount").asLong()),
                    LocalDateTime.parse(node.get("cancelledAt").asString())
                ));
                saleRecordRepository.findById(saleRecordId).ifPresent(sr -> {
                    if (sr.status != SaleRecordStatus.CANCELLED) {
                        sr.cancel();
                    }
                });
            }
        }
    }
}
