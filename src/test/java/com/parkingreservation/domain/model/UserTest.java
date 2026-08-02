package com.parkingreservation.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UserTest {

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User(1L, " ", false, false));
    }

    @Test
    void rejectsNullName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User(1L, null, false, false));
    }
}
