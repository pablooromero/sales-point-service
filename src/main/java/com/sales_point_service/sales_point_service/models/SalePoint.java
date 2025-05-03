package com.sales_point_service.sales_point_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalePoint {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String name;
}
