package com.sales_point_service.sales_point_service.models;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CostId implements Serializable {
    private Long fromId;
    private Long toId;
    public CostId() {}

    public CostId(Long fromId, Long toId) {
        this.fromId = fromId;
        this.toId = toId;
    }

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public Long getToId() {
        return toId;
    }

    public void setToId(Long toId) {
        this.toId = toId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CostId)) return false;
        CostId that = (CostId) o;

        return Objects.equals(fromId, that.fromId) &&
                Objects.equals(toId, that.toId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId);
    }
}
