package com.ludot.protocol;


/** The active mystery cell and how long it lasts. */
public record MysteryCellDto(int cell, int roundsLeft) implements java.io.Serializable {

    public MysteryCellDto {

    }
}
