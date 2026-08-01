package com.parkingreservation.application;

import com.parkingreservation.domain.policy.EvParkingPolicy;
import com.parkingreservation.domain.policy.ReservationPolicy;
import com.parkingreservation.domain.policy.StandardParkingPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReservationPolicyConfiguration {

    @Bean
    public ReservationPolicy standardParkingPolicy() {
        return new StandardParkingPolicy();
    }

    @Bean
    public ReservationPolicy evParkingPolicy() {
        return new EvParkingPolicy();
    }
}
