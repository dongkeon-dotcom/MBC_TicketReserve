package com.mbc.admin.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class VenueSeatMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long masterSeatId;

    private String seatNumber;
    private Integer xPos;
    private Integer yPos;
    private String blockName;
}