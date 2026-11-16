package com.donsang.dons.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.donsang.dons.Fixtures;
import com.donsang.dons.domaine.Hopital;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET = "secret-factice-reserve-aux-tests-0123456789";

    private final JwtService jwt = new JwtService(SECRET, Duration.ofMinutes(5));

    private Hopital hopital() {
        Hopital h = Fixtures.hopital();
        h.setId(7L);
        return h;
    }

    @Test
    void leJetonContientIdentifiantEtRole() {
        Claims claims = jwt.lire(jwt.generer(hopital()));
        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("HOPITAL");
    }

    @Test
    void unJetonSigneAvecUneAutreCleEstRejete() {
        JwtService autre = new JwtService("une-autre-cle-secrete-de-test-0123456789", Duration.ofMinutes(5));
        String jetonEtranger = autre.generer(hopital());
        assertThatThrownBy(() -> jwt.lire(jetonEtranger)).isInstanceOf(JwtException.class);
    }

    @Test
    void unJetonExpireEstRejete() {
        JwtService courte = new JwtService(SECRET, Duration.ofSeconds(-1));
        String expire = courte.generer(hopital());
        assertThatThrownBy(() -> jwt.lire(expire)).isInstanceOf(ExpiredJwtException.class);
    }
}
