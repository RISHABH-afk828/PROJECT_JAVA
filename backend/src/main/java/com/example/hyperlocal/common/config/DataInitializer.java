package com.example.hyperlocal.common.config;

import com.example.hyperlocal.address.entity.Address;
import com.example.hyperlocal.address.repository.AddressRepository;
import com.example.hyperlocal.category.entity.Category;
import com.example.hyperlocal.category.repository.CategoryRepository;
import com.example.hyperlocal.inventory.entity.Inventory;
import com.example.hyperlocal.inventory.entity.InventoryMovement;
import com.example.hyperlocal.inventory.repository.InventoryMovementRepository;
import com.example.hyperlocal.inventory.repository.InventoryRepository;
import com.example.hyperlocal.product.entity.Product;
import com.example.hyperlocal.product.repository.ProductRepository;
import com.example.hyperlocal.user.entity.Role;
import com.example.hyperlocal.user.entity.User;
import com.example.hyperlocal.user.repository.UserRepository;
import com.example.hyperlocal.vendor.entity.StoreHours;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.entity.VendorStatus;
import com.example.hyperlocal.vendor.repository.StoreHoursRepository;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final StoreHoursRepository storeHoursRepository;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            VendorRepository vendorRepository,
            StoreHoursRepository storeHoursRepository,
            AddressRepository addressRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.storeHoursRepository = storeHoursRepository;
        this.addressRepository = addressRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data seed.");
            return;
        }

        log.info("Seeding initial seed data for Hyperlocal Marketplace...");

        // 1. Admin
        User admin = new User("System Admin", "admin@hyperlocal.com", "+919000000001", passwordEncoder.encode("Admin@123"), Role.ADMIN);
        userRepository.save(admin);

        // 2. Customer
        User customer = new User("Rishabh Customer", "customer@hyperlocal.com", "+919000000002", passwordEncoder.encode("Customer@123"), Role.CUSTOMER);
        User savedCustomer = userRepository.save(customer);

        // Seed Customer Default Address (Koramangala)
        Address custAddr = new Address();
        custAddr.setUserId(savedCustomer.getId());
        custAddr.setLabel("Home");
        custAddr.setHouse("Flat 402, Green Glen Apartments");
        custAddr.setStreet("1st Cross, 4th Block");
        custAddr.setLocality("Koramangala");
        custAddr.setCity("Bengaluru");
        custAddr.setState("Karnataka");
        custAddr.setPostalCode("560034");
        custAddr.setLatitude(12.9352);
        custAddr.setLongitude(77.6245);
        custAddr.setIsDefault(true);
        custAddr.setDeliveryInstructions("Ring doorbell and leave at doorstep.");
        addressRepository.save(custAddr);

        // 3. Delivery Partner
        User delivery = new User("Kiran Delivery Partner", "delivery@hyperlocal.com", "+919000000003", passwordEncoder.encode("Delivery@123"), Role.DELIVERY_PARTNER);
        userRepository.save(delivery);

        // 4. Vendors
        // Vendor 1: Fresh Mart (Koramangala)
        User vOwner1 = new User("Suresh FreshMart", "freshmart@hyperlocal.com", "+919000000004", passwordEncoder.encode("Vendor@123"), Role.VENDOR);
        userRepository.save(vOwner1);

        Vendor v1 = new Vendor(
                vOwner1.getId(),
                "Fresh Mart Supermarket",
                "Wide assortment of groceries, fresh dairy, pulses, grains and daily home essentials.",
                "+919845012345",
                "88, 80 Feet Road, 4th Block, Koramangala, Bengaluru",
                12.9348,
                77.6239,
                5.0
        );
        v1.setStatus(VendorStatus.ACTIVE);
        Vendor savedV1 = vendorRepository.save(v1);
        seedStoreHours(savedV1.getId());

        // Vendor 2: Daily Needs Express (Indiranagar)
        User vOwner2 = new User("Rajesh DailyNeeds", "dailyneeds@hyperlocal.com", "+919000000005", passwordEncoder.encode("Vendor@123"), Role.VENDOR);
        userRepository.save(vOwner2);

        Vendor v2 = new Vendor(
                vOwner2.getId(),
                "Daily Needs Express",
                "Snacks, chilled beverages, dairy products, breads and quick essentials.",
                "+919845054321",
                "45, 100 Feet Road, Indiranagar, Bengaluru",
                12.9780,
                77.6402,
                4.5
        );
        v2.setStatus(VendorStatus.ACTIVE);
        Vendor savedV2 = vendorRepository.save(v2);
        seedStoreHours(savedV2.getId());

        // Vendor 3: Organic Greens (HSR Layout)
        User vOwner3 = new User("Amit OrganicGreens", "organicgreens@hyperlocal.com", "+919000000006", passwordEncoder.encode("Vendor@123"), Role.VENDOR);
        userRepository.save(vOwner3);

        Vendor v3 = new Vendor(
                vOwner3.getId(),
                "Organic Greens & Produce",
                "Direct farm-fresh vegetables, organic produce, seasonal fruits and greens.",
                "+919845098765",
                "12, 27th Main, Sector 1, HSR Layout, Bengaluru",
                12.9125,
                77.6441,
                6.0
        );
        v3.setStatus(VendorStatus.ACTIVE);
        Vendor savedV3 = vendorRepository.save(v3);
        seedStoreHours(savedV3.getId());

        // 5. Seed Categories
        Category catDairy = categoryRepository.save(new Category("Dairy & Eggs", "dairy", null));
        Category catGroceries = categoryRepository.save(new Category("Groceries & Staples", "groceries", null));
        Category catProduce = categoryRepository.save(new Category("Fruits & Veggies", "produce", null));
        Category catSnacks = categoryRepository.save(new Category("Bakery & Snacks", "snacks", null));
        Category catBeverages = categoryRepository.save(new Category("Beverages", "beverages", null));

        // 6. Seed Products & Inventory for Fresh Mart (Vendor 1)
        seedProduct(savedV1.getId(), catDairy.getId(), "Farm Fresh Whole Milk 500ml", new BigDecimal("32.00"), "500 ml",
                "Pure pasteurized whole milk, sourced from local farms daily.", 25, 5);
        seedProduct(savedV1.getId(), catSnacks.getId(), "Whole Wheat Sandwich Bread", new BigDecimal("45.00"), "400 g",
                "Soft, freshly baked 100% whole wheat bread loaf.", 15, 3);
        seedProduct(savedV1.getId(), catDairy.getId(), "Organic Farm Eggs (Pack of 6)", new BigDecimal("65.00"), "6 pcs",
                "Nutritious free-range farm fresh brown eggs.", 12, 4);
        seedProduct(savedV1.getId(), catGroceries.getId(), "Premium Aged Basmati Rice", new BigDecimal("140.00"), "1 kg",
                "Aromatic long-grain aged basmati rice for biryanis and daily meals.", 30, 8);
        seedProduct(savedV1.getId(), catGroceries.getId(), "Pure Cow Desi Ghee 500ml", new BigDecimal("360.00"), "500 ml",
                "Traditional bilona method pure cow ghee with rich golden aroma.", 8, 2);

        // Seed Products for Daily Needs Express (Vendor 2)
        seedProduct(savedV2.getId(), catSnacks.getId(), "Classic Salted Potato Chips", new BigDecimal("20.00"), "50 g",
                "Crispy golden sliced potato chips with sea salt.", 40, 10);
        seedProduct(savedV2.getId(), catBeverages.getId(), "Cold Pressed Valencia Orange Juice", new BigDecimal("85.00"), "300 ml",
                "No added sugar, 100% pure cold pressed orange juice.", 10, 3);
        seedProduct(savedV2.getId(), catDairy.getId(), "Delicious Table Butter 100g", new BigDecimal("58.00"), "100 g",
                "Pasteurized creamy salted table butter.", 18, 5);

        // Seed Products for Organic Greens & Produce (Vendor 3)
        seedProduct(savedV3.getId(), catProduce.getId(), "Fresh Organic Tomatoes", new BigDecimal("40.00"), "1 kg",
                "Vine-ripened organic red tomatoes.", 50, 10);
        seedProduct(savedV3.getId(), catProduce.getId(), "Farm Fresh Spinach (Palak)", new BigDecimal("25.00"), "250 g",
                "Crisp green hydroponic spinach bunch.", 30, 5);
        seedProduct(savedV3.getId(), catProduce.getId(), "Fresh Crisp Apples", new BigDecimal("180.00"), "1 kg",
                "Sweet and juicy Shimla red apples.", 20, 5);

        log.info("Initial data seeding completed successfully with categories, products, inventory, 3 vendors, 1 customer, 1 delivery partner, 1 admin!");
    }

    private void seedProduct(Long vendorId, Long categoryId, String name, BigDecimal price, String unit, String desc, int stock, int threshold) {
        Product p = new Product(vendorId, categoryId, name, price, unit);
        p.setDescription(desc);
        p.setSlug(name.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        p.setActive(true);
        Product saved = productRepository.save(p);

        Inventory inv = new Inventory(saved.getId(), stock, threshold);
        inventoryRepository.save(inv);

        InventoryMovement movement = new InventoryMovement(
                saved.getId(), stock, stock, "RESTOCK", "INITIAL_STOCK", "INIT", 1L);
        movementRepository.save(movement);
    }

    private void seedStoreHours(Long vendorId) {
        for (int day = 1; day <= 7; day++) {
            StoreHours hours = new StoreHours(vendorId, day, LocalTime.of(0, 0), LocalTime.of(23, 59, 59), true);
            storeHoursRepository.save(hours);
        }
    }
}
