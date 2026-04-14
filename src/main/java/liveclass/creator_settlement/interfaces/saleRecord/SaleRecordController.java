package liveclass.creator_settlement.interfaces.saleRecord;

import jakarta.validation.Valid;
import liveclass.creator_settlement.app.saleRecord.SaleRecordService;
import liveclass.creator_settlement.app.saleRecord.dto.SaleRecordCreateReq;
import liveclass.creator_settlement.app.saleRecord.dto.SaleRecordRes;
import liveclass.creator_settlement.global.page.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sale-record")
public class SaleRecordController {

    private final SaleRecordService saleRecordService;

    @PostMapping(version = "v1")
    public ResponseEntity<String> register(@Valid @RequestBody SaleRecordCreateReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleRecordService.register(req));
    }

    @GetMapping(version = "v1")
    public PageResponse<SaleRecordRes> getSaleRecords(
            @RequestParam String creatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return PageResponse.from(saleRecordService.getSaleRecords(creatorId, startDate, endDate, pageable));
    }
}
