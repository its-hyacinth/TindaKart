package com.ddev.tindakart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:tindakart;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.sql.init.mode=never",
      "tindakart.bootstrap.admin-username=",
      "tindakart.bootstrap.admin-password="
})
class TindaKartApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
