package org.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Address embeddable entity for user addresses
 * Maps to address columns in the users table
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(name = "address_line1", nullable = false)
    private String line1;

    @Column(name = "address_line2")
    private String line2;

    @Column(name = "address_line3")
    private String line3;

    @Column(name = "address_town", nullable = false)
    private String town;

    @Column(name = "address_county", nullable = false)
    private String county;

    @Column(name = "address_postcode", nullable = false)
    private String postcode;
}
