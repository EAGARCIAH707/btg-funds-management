package com.btg.funds.domain.port.out;

import com.btg.funds.domain.model.Fund;

import java.util.Optional;

public interface FundRepository {

    Optional<Fund> findById(String fundId);
}
