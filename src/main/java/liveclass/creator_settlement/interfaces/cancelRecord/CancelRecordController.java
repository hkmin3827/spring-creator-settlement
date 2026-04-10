package liveclass.creator_settlement.interfaces.cancelRecord;

import jakarta.validation.Valid;
import liveclass.creator_settlement.app.cancelRecord.CancelRecordService;
import liveclass.creator_settlement.app.cancelRecord.dto.CancelRecordCreateReq;
import liveclass.creator_settlement.app.cancelRecord.dto.CancelRecordRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cancel-record")
public class CancelRecordController {

    private final CancelRecordService cancelRecordService;

    @PostMapping(version = "v1")
    @ResponseStatus(HttpStatus.CREATED)
    public CancelRecordRes register(@Valid @RequestBody CancelRecordCreateReq req) {
        return cancelRecordService.register(req);
    }
}
