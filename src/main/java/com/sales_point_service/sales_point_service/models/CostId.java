package com.sales_point_service.sales_point_service.models;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CostId implements Serializable {

    private Long fromId;

    private Long toId;

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
