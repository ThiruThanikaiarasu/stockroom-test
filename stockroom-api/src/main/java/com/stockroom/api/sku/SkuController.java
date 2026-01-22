package com.stockroom.api.sku;

import java.util.List;

import com.stockroom.core.sku.SkuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skus")
public class SkuController {

    private final SkuService skus;

    public SkuController(SkuService skus) {
        this.skus = skus;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SkuResponse create(@Valid @RequestBody CreateSkuRequest request) {
        return SkuResponse.from(skus.create(request.toNewSku()));
    }

    @GetMapping
    public List<SkuResponse> list() {
        return skus.listActive().stream().map(SkuResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SkuResponse get(@PathVariable Long id) {
        return SkuResponse.from(skus.get(id));
    }

    @PatchMapping("/{id}")
    public SkuResponse patch(@PathVariable Long id, @Valid @RequestBody PatchSkuRequest request) {
        return SkuResponse.from(skus.patch(id, request.toPatch()));
    }
}
