package liveclass.creator_settlement.interfaces.saleRecord;

import jakarta.validation.Valid;
import liveclass.creator_settlement.app.saleRecord.SaleRecordService;
import liveclass.creator_settlement.app.saleRecord.dto.SaleRecordCreateReq;
import liveclass.creator_settlement.app.saleRecord.dto.SaleRecordRes;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sale-record")
public class SaleRecordController {

    private final SaleRecordService saleRecordService;

    @PostMapping(version = "v1")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleRecordRes register(@Valid @RequestBody SaleRecordCreateReq req) {
        return saleRecordService.register(req);
    }

    @GetMapping(version = "v1")
    public List<SaleRecordRes> list(
            @RequestParam String creatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return saleRecordService.list(creatorId, startDate, endDate);
    }
}
