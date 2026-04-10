package liveclass.creator_settlement.interfaces.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlement")   // 컨트롤러 메서드 버저닝 적용 예정 ex. @POSTMAPPING(version="v1")
public class SettlementController {
}
