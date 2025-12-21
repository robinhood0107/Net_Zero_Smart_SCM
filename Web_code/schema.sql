--주석으로 좀 보기 편하게 수정함.

-- 1. Shipyard (조선소)
CREATE TABLE Shipyard (
    ShipyardID INT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Region VARCHAR(100),
    HName VARCHAR(100), -- 담당 본부명
    EstablishedDate DATE
);

-- 2. User (뒤에서 외래키 넣을 예정이라 앞으로 배치함)
CREATE TABLE Users (
    UserID INT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Email VARCHAR(100) UNIQUE NOT NULL,
    AffiliationType VARCHAR(20) CHECK (AffiliationType IN ('SHIPYARD', 'SUPPLIER', 'AUDITOR')),
    Role VARCHAR(50),
    ShipyardID INT,
    SupplierID INT,
    FOREIGN KEY (ShipyardID) REFERENCES Shipyard(ShipyardID) ON DELETE SET NULL
    -- SupplierID 외래키는 Supplier 테이블 생성 후 추가하려고 함 (잊지 말기)
);

-- 3. Supplier (공급업체)
CREATE TABLE Supplier (
    SupplierID INT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Country VARCHAR(50),
    ContactName VARCHAR(100),
    ContactPhone VARCHAR(50),
    ESGGrade CHAR(1) CHECK (ESGGrade IN ('A', 'B', 'C', 'D'))
);

-- User 테이블에 Supplier FK 추가 (앞에서 FK 추가한다고 한 것것)
ALTER TABLE Users ADD CONSTRAINT FK_Users_Supplier
FOREIGN KEY (SupplierID) REFERENCES Supplier(SupplierID) ON DELETE SET NULL;

-- 4. Part (부품)
CREATE TABLE Part (
    PartID INT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Spec VARCHAR(255),
    Unit VARCHAR(20),
    BaseEmissionFactor FLOAT DEFAULT 0 -- 기본 탄소배출계수
);

-- 5. ShipProject (선박 프로젝트)
CREATE TABLE ShipProject (
    ProjectID INT PRIMARY KEY,
    ShipName VARCHAR(100) NOT NULL,
    ShipType VARCHAR(50) CHECK (ShipType IN ('Bulk carrier', 'Containership', 'Tanker', 'Gas carrier', 'General cargo ship', 'Refrigerated cargo carrier', 'Ro-ro', 'Passenger ship', 'Offshore supply vessel')),
    ContractDate DATE,
    DeliveryDueDate DATE,
    Status VARCHAR(20) CHECK (Status IN ('설계', '건조중', '인도완료', '취소')),
    ShipyardID INT NOT NULL,
    FOREIGN KEY (ShipyardID) REFERENCES Shipyard(ShipyardID)
);

-- 6. SupplierPart (부품-공급업체 관계 / 단가 정보)
CREATE TABLE SupplierPart (
    SupplierID INT,
    PartID INT,
    UnitPrice FLOAT NOT NULL CHECK (UnitPrice >= 0),
    LeadTimeDays INT CHECK (LeadTimeDays >= 0),
    MinOrderQty INT CHECK (MinOrderQty >= 0),
    PRIMARY KEY (SupplierID, PartID),
    FOREIGN KEY (SupplierID) REFERENCES Supplier(SupplierID),
    FOREIGN KEY (PartID) REFERENCES Part(PartID)
);

-- 7. Warehouse (창고)
CREATE TABLE Warehouse (
    WarehouseID INT PRIMARY KEY,
    ShipyardID INT NOT NULL,
    Name VARCHAR(100),
    Location VARCHAR(255),
    IsTempControlled BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (ShipyardID) REFERENCES Shipyard(ShipyardID)
);

-- 8. Inventory (재고)
CREATE TABLE Inventory (
    WarehouseID INT,
    PartID INT,
    Quantity INT NOT NULL DEFAULT 0 CHECK (Quantity >= 0),
    PRIMARY KEY (WarehouseID, PartID),
    FOREIGN KEY (WarehouseID) REFERENCES Warehouse(WarehouseID),
    FOREIGN KEY (PartID) REFERENCES Part(PartID)
);

-- 9. PurchaseOrder (발주서)
CREATE TABLE PurchaseOrder (
    POID INT PRIMARY KEY,
    OrderDate DATE NOT NULL,
    Status VARCHAR(20) CHECK (Status IN ('요청', '발주완료', '취소', '검수중')),
    EngineerName VARCHAR(100),
    ProjectID INT NOT NULL,
    SupplierID INT NOT NULL,
    FOREIGN KEY (ProjectID) REFERENCES ShipProject(ProjectID),
    FOREIGN KEY (SupplierID) REFERENCES Supplier(SupplierID)
);

-- 10. PurchaseOrderLine (발주 항목)
CREATE TABLE PurchaseOrderLine (
    POID INT,
    LineNo INT,
    PartID INT NOT NULL,
    Quantity INT NOT NULL CHECK (Quantity > 0),
    UnitPriceAtOrder FLOAT NOT NULL CHECK (UnitPriceAtOrder >= 0),
    RequestedDueDate DATE,
    PRIMARY KEY (POID, LineNo),
    FOREIGN KEY (POID) REFERENCES PurchaseOrder(POID) ON DELETE CASCADE,
    FOREIGN KEY (PartID) REFERENCES Part(PartID)
);

-- 11. Delivery (납품)
CREATE TABLE Delivery (
    DeliveryID INT PRIMARY KEY,
    POID INT NOT NULL,
    ActualArrivalDate DATE,
    TransportMode VARCHAR(50),
    DistanceKm FLOAT CHECK (DistanceKm >= 0),
    Status VARCHAR(20) CHECK (Status IN ('정상입고', '부분입고', '지연')),
    FOREIGN KEY (POID) REFERENCES PurchaseOrder(POID)
);

-- 12. DeliveryLine (납품 상세 / 입고 기록)
CREATE TABLE DeliveryLine (
    DeliveryID INT,
    POID INT,
    LineNo INT,
    ReceivedQty INT NOT NULL CHECK (ReceivedQty >= 0),
    InspectionResult VARCHAR(255),

    PRIMARY KEY (DeliveryID, POID, LineNo),
    FOREIGN KEY (DeliveryID) REFERENCES Delivery(DeliveryID) ON DELETE CASCADE,
    CONSTRAINT FK_DeliveryLine_POLine
        FOREIGN KEY (POID, LineNo) REFERENCES PurchaseOrderLine(POID, LineNo)
);

-- 13. CarbonEmissionRecord (탄소배출 기록)
CREATE TABLE CarbonEmissionRecord (
    RecordID INT PRIMARY KEY,
    DeliveryID INT,
    ProjectID INT,
    EmissionType VARCHAR(50) NOT NULL, -- 예: 운송, 보관, 생산
    CO2eAmount FLOAT NOT NULL CHECK (CO2eAmount >= 0),
    Basis VARCHAR(100), -- 산정 기준 (표준, 자체계산 등)
    RecordDate DATE NOT NULL,

    FOREIGN KEY (DeliveryID) REFERENCES Delivery(DeliveryID) ON DELETE SET NULL,
    FOREIGN KEY (ProjectID) REFERENCES ShipProject(ProjectID) ON DELETE SET NULL,

    CONSTRAINT CHK_Emission_Link CHECK (DeliveryID IS NOT NULL OR ProjectID IS NOT NULL)
);


