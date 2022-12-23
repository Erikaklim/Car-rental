CREATE TABLE Customer(
    id BIGSERIAL NOT NULL PRIMARY KEY,
    personal_code CHAR(11) NOT NULL CHECK(personal_code ~ '^[1-6][0-9]{10}$'),
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    date_of_birth DATE NOT NULL CHECK((EXTRACT('year' FROM AGE(date_of_birth))::int) >= 18),
    address VARCHAR(30) NOT NULL,
    phone_number CHAR(9) NOT NULL UNIQUE CHECK(phone_number ~ '^86[0-9]{7}$')
);


CREATE TABLE Location(
    id BIGSERIAL NOT NULL PRIMARY KEY,
    city VARCHAR(20) DEFAULT 'Vilnius' NOT NULL,
    address VARCHAR(30) DEFAULT 'Didlaukio g. 47' NOT NULL, 
    phone_number CHAR(9) NOT NULL UNIQUE CHECK (phone_number ~ '^86[0-9]{7}$')
);


CREATE TABLE Car(
    id BIGSERIAL NOT NULL PRIMARY KEY,
    license_number CHAR(6) NOT NULL CHECK(license_number ~ '^[A-Z]{3}[0-9]{3}$'),
    make VARCHAR(20) NOT NULL,
    model VARCHAR(20) NOT NULL,
    price DECIMAL(5, 2) NOT NULL,
    status CHAR(20) CHECK(status IN ('available', 'unavailable', 'in use', 'being processed')) 
                    DEFAULT 'unavailable' NOT NULL,    
    location_id BIGSERIAL REFERENCES Location(id) NOT NULL
);

CREATE TABLE Reservation(
    car_id BIGSERIAL NOT NULL PRIMARY KEY REFERENCES Car(id),
    customer_id BIGSERIAL NOT NULL REFERENCES Customer(id) ON DELETE CASCADE,
    pickup_date DATE CHECK(pickup_date >= CURRENT_DATE) DEFAULT CURRENT_DATE NOT NULL,
    return_date DATE CHECK (return_date >= pickup_date AND return_date <= (pickup_date + 30))
                DEFAULT (CURRENT_DATE + 30) NOT NULL
);

CREATE UNIQUE INDEX personal_code_index
ON Customer (personal_code);

CREATE INDEX first_name_index
ON Customer (first_name);

CREATE INDEX last_name_index
ON Customer (last_name);

CREATE UNIQUE INDEX license_number_index
ON Car (license_number);

CREATE FUNCTION is_car_available()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT status FROM Car WHERE id = NEW.car_id ) <> 'available'
    THEN RAISE EXCEPTION 'Car is unavailable';
    END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql; 

CREATE TRIGGER Only_available_car_reservation
BEFORE INSERT OR UPDATE OF car_id ON Reservation
FOR EACH ROW
EXECUTE PROCEDURE is_car_available();


CREATE FUNCTION status_being_processed()
RETURNS TRIGGER AS $$
BEGIN   
    UPDATE Car SET status = 'being processed'
    WHERE id = OLD.car_id;
RETURN NEW;
END;
$$ LANGUAGE plpgsql; 

CREATE TRIGGER Changing_car_status_to_being_processed
AFTER DELETE ON Reservation
FOR EACH ROW
EXECUTE PROCEDURE status_being_processed();

CREATE FUNCTION status_in_use()
RETURNS TRIGGER AS $$
BEGIN
    IF(SELECT status FROM Car WHERE id = OLD.car_id) = 'in use' THEN
    UPDATE Car SET status = 'being processed'
    WHERE id = OLD.car_id;
    END IF;
    UPDATE Car SET status = 'in use' 
    WHERE id = NEW.car_id;
RETURN NEW;
END;
$$ LANGUAGE plpgsql; 

CREATE TRIGGER Status_in_use
AFTER INSERT OR UPDATE OF car_id ON Reservation
FOR EACH ROW
EXECUTE PROCEDURE status_in_use();

CREATE FUNCTION can_update_car()
RETURNS TRIGGER AS $$
BEGIN
    IF(SELECT status FROM Car WHERE id = NEW.id )  = 'in use' 
    THEN RAISE EXCEPTION 'Car can not be updated because it is in use';
    END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER Can_update_car
BEFORE UPDATE ON Car
FOR EACH ROW
EXECUTE PROCEDURE can_update_car();


CREATE VIEW Reservations_and_discounts AS
SELECT customer_id, COUNT(car_id) AS reservations,
    CASE WHEN COUNT(car_id) > 2 THEN 20 
        WHEN COUNT(car_id) > 1 THEN 10 
        ELSE 0 END AS discount_percent
FROM Reservation
GROUP BY customer_id;


CREATE VIEW Total_days_and_price AS
SELECT Reservation.customer_id, car_id, return_date - pickup_date + 1 AS total_days,
    ROUND((return_date - pickup_date + 1) * price * (1 - discount_percent*.01), 2) AS total_price 
FROM Reservation
JOIN Reservations_and_discounts ON Reservation.customer_id = Reservations_and_discounts.customer_id
JOIN Car ON Car.id = Reservation.car_id;


insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('50202280302', 'Randy', 'Tassel', '1958-01-18', '2524 7th Junction', '866214268');
insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('50202280102', 'Donal', 'Izatt', '1997-07-15', '4 Oxford Terrace', '863909634');
insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('60203021111', 'Bellina', 'Brakewell', '1974-02-16', '60 Tomscot Road', '862940461');
insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('60206301011', 'Ivette', 'Dunham', '1959-01-22', '94 Kenwood Way', '862957172');
insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('50205301031', 'Ruthy', 'Fluger', '1980-10-27', '44873 Merry Lane', '864965708');
insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('50212310031', 'Rawley', 'Elcoat', '1983-07-18', '29174 Center Trail', '869815161');
insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('60211300131', 'Elisabeth', 'Adamini', '1952-07-04', '314 Fairview Terrace', '866889890');
insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('60111010055', 'Aurea', 'Yesinin', '1965-10-05', '3 Rowland Circle', '862927670');
insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('50303010055', 'Guendolen', 'Popplestone', '1991-07-07', '63835 Prairieview Park', '867941108');
insert into Customer (personal_code, first_name, last_name, date_of_birth, address, phone_number) values ('50111070066','Haydon', 'Syne', '1991-04-14', '6149 Kenwood Point', '869722601');

insert into Location (city, address, phone_number) values ('Vilnius', 'Didlaukio g. 47', '865042886');
insert into Location (city, address, phone_number) values ('Vilnius', '63054 Bluejay Point', '863070536');
insert into Location (city, address, phone_number) values ('Vilnius', '3 Bay Terrace', '866598423');
insert into Location (city, address, phone_number) values ('Vilnius', '91 Sloan Pass', '865411100');
insert into Location (city, address, phone_number) values ('Klaipeda', '0 Dorton Parkway', '867762647');
insert into Location (city, address, phone_number) values ('Klaipeda', '8354 Laurel Place', '863035628');
insert into Location (city, address, phone_number) values ('Kaunas', '4 Acker Junction', '861923361');
insert into Location (city, address, phone_number) values ('Kaunas', '7 Bashford Center', '862045801');
insert into Location (city, address, phone_number) values ('Siauliai', '1 Del Sol Junction', '863843685');
insert into Location (city, address, phone_number) values ('Panevezys', '11 5th Place', '861059075');

insert into Car (license_number, make, model, price, status, location_id) values ('ASN678', 'Ford', 'Thunderbird', 52.56 ,'available', 1);
insert into Car (license_number, make, model, price, status, location_id) values ('JSU879', 'Chevrolet', 'Silverado 3500', 60.00, 'available', 2);
insert into Car (license_number, make, model, price, status, location_id) values ('HGH789', 'Dodge', 'Viper', 20.04, 'unavailable', 3);
insert into Car (license_number, make, model, price, status, location_id) values ('LMN102', 'Subaru', 'SVX', 72.78, 'available', 4);
insert into Car (license_number, make, model, price, status, location_id) values ('SJK345', 'Ford', 'LTD Crown Victoria', 34.99, 'available', 5);
insert into Car (license_number, make, model, price, status, location_id) values ('LNJ567', 'Toyota', 'Prius v', 35.12, 'available', 6);
insert into Car (license_number, make, model, price, status, location_id) values ('SMN789', 'Dodge', 'Viper', 20.04, 'available', 7);
insert into Car (license_number, make, model, price, status, location_id) values ('BSJ679', 'Ford', 'E-Series', 45.74, 'available', 8);
insert into Car (license_number, make, model, price, status, location_id) values ('DAU789', 'Nissan', 'Sentra', 54.99, 'available', 9);
insert into Car (license_number, make, model, price, status, location_id) values ('CSV100', 'Ford', 'F150', 77.09, 'available', 10);
insert into Car (license_number, make, model, price, status, location_id) values ('SIS752', 'Nissan', '370Z', 85.45, 'available', 7);
insert into Car (license_number, make, model, price, status, location_id) values ('FJG860', 'Suzuki', 'Aerio', 60.50, 'available', 4);
insert into Car (license_number, make, model, price, status, location_id) values ('IJK900', 'Subaru', 'Brat', 83.56, 'available', 1);
insert into Car (license_number, make, model, price, status, location_id) values ('OPP743', 'GMC', '1500 Club Coupe', 50, 'available', 2);
insert into Car (license_number, make, model, price, status, location_id) values ('LMO090', 'Volvo', 'V50', 70.67, 'available', 3);
insert into Car (license_number, make, model, price, status, location_id) values ('JKL098', 'Mercury', 'Milan', 53.78, 'available', 5);
insert into Car (license_number, make, model, price, status, location_id) values ('IMN321', 'Ford', 'F-Series', 18.69, 'available', 6);
insert into Car (license_number, make, model, price, status, location_id) values ('LJK890', 'Porsche', '911', 25.00, 'being processed', 10);
insert into Car (license_number, make, model, price, status, location_id) values ('TUM436', 'Ford', 'E-Series', 45.74, 'available', 2);
insert into Car (license_number, make, model, price, status, location_id) values ('WBS765', 'Saab', '900', 60.50, 'unavailable', 5);

insert into Reservation (car_id, customer_id, pickup_date, return_date) values (4, 1, '2023-01-23', '2023-01-27');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (5, 2, '2023-01-23', '2023-01-27');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (6, 3, '2023-01-16', '2023-01-27');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (7, 4, '2023-01-15', '2023-01-15');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (8, 5, '2023-01-13', '2023-02-07');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (9, 6, '2023-01-13', '2023-02-07');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (11, 7, '2023-02-23', '2023-02-27');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (12, 8, '2023-01-23', '2023-01-29');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (13, 9, '2023-01-02', '2023-01-29');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (14, 9, '2023-02-23', '2023-02-25');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (15, 10, '2023-01-09', '2023-01-11');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (16, 10, '2023-01-23', '2023-01-29');
insert into Reservation (car_id, customer_id, pickup_date, return_date) values (17, 10, '2023-01-23', '2023-01-30');

CREATE MATERIALIZED VIEW Car_categories (id, category)
AS SELECT id, CASE WHEN price >= 80 THEN 'expensive'
                    WHEN price >= 40 THEN 'mid-range'
                    ELSE 'cheap' END AS price_category
FROM Car 
ORDER BY 2
WITH DATA;






