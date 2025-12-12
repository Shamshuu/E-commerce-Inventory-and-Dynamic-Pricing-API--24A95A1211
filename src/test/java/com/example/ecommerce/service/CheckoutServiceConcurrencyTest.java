package test.java.com.example.ecommerce.service;

import com.example.ecommerce.entity.Variant;
import com.example.ecommerce.entity.Reservation;
import com.example.ecommerce.repository.VariantRepository;
import com.example.ecommerce.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CheckoutServiceConcurrencyTest {
    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private VariantRepository variantRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @Transactional
    public void testConcurrentCheckoutPreventsOversell() throws InterruptedException {
        // Setup: create a variant with 5 in stock, 2 reservations of 3 each (overlapping)
        Variant variant = new Variant();
        variant.setSku("TEST-SKU");
        variant.setTitle("Test Variant");
        variant.setStockQuantity(5);
        variant.setReservedQuantity(6);
        variant.setPriceAdjustment(0.0);
        variant = variantRepository.save(variant);

        Reservation r1 = new Reservation();
        r1.setVariantId(variant.getId());
        r1.setQuantity(3);
        r1.setReleased(false);
        r1 = reservationRepository.save(r1);

        Reservation r2 = new Reservation();
        r2.setVariantId(variant.getId());
        r2.setQuantity(3);
        r2.setReleased(false);
        r2 = reservationRepository.save(r2);

        // Simulate two concurrent checkouts
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        Runnable checkout1 = () -> {
            try {
                checkoutService.checkoutCart(1L, Arrays.asList(r1.getId()), 100.0);
            } catch (Exception ignored) {}
            latch.countDown();
        };
        Runnable checkout2 = () -> {
            try {
                checkoutService.checkoutCart(1L, Arrays.asList(r2.getId()), 100.0);
            } catch (Exception ignored) {}
            latch.countDown();
        };
        executor.submit(checkout1);
        executor.submit(checkout2);
        latch.await();
        executor.shutdown();

        // Only one reservation should succeed, the other should fail due to insufficient stock
        Variant updated = variantRepository.findById(variant.getId()).orElseThrow();
        assertTrue(updated.getStockQuantity() >= 0);
        assertTrue(updated.getStockQuantity() <= 2);
    }
}
