package org.morago.dto.review;


public record ReviewResponse (

    Long id,

    Integer rating,

    String comment

) {

}