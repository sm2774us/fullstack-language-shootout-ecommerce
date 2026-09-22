#pragma once
#include <string>
#include <vector>
struct Product { std::string sku, name, description, category; long long price_cents; int stock_quantity; };
inline std::vector<Product> seed_products() {
    return {
        {"SKU-1001","Wireless Mechanical Keyboard","Hot-swappable switches, USB-C.","electronics",8999,42},
        {"SKU-1002","27in 4K Monitor","IPS panel, 144Hz.","electronics",39999,15},
        {"SKU-1003","Ceramic Pour-Over Kettle","1.2L, gooseneck spout.","home",4599,88},
        {"SKU-1004","Trail Running Shoes","Lightweight, grippy outsole.","apparel",12999,60},
        {"SKU-1005","Stainless Steel Water Bottle","32oz, insulated.","home",2999,120},
    };
}
