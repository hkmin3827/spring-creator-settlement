package liveclass.creator_settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CreatorSettlementApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreatorSettlementApplication.class, args);
	}

}
