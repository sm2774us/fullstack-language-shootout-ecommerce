(* Business logic — hidden from the outside world. *)
type product = {
  sku : string;
  name : string;
  description : string;
  price_cents : int;
  category : string;
  stock_quantity : int;
}

let seed_products () = [
  { sku = "SKU-1001"; name = "Wireless Mechanical Keyboard"; description = "Hot-swappable switches, USB-C."; price_cents = 8999; category = "electronics"; stock_quantity = 42 };
  { sku = "SKU-1002"; name = "27in 4K Monitor"; description = "IPS panel, 144Hz."; price_cents = 39999; category = "electronics"; stock_quantity = 15 };
  { sku = "SKU-1003"; name = "Ceramic Pour-Over Kettle"; description = "1.2L, gooseneck spout."; price_cents = 4599; category = "home"; stock_quantity = 88 };
  { sku = "SKU-1004"; name = "Trail Running Shoes"; description = "Lightweight, grippy outsole."; price_cents = 12999; category = "apparel"; stock_quantity = 60 };
  { sku = "SKU-1005"; name = "Stainless Steel Water Bottle"; description = "32oz, insulated."; price_cents = 2999; category = "home"; stock_quantity = 120 };
]

let list ?category () =
  let products = seed_products () in
  match category with
  | None | Some "" -> products
  | Some c -> List.filter (fun p -> p.category = c) products

let get sku = List.find_opt (fun p -> p.sku = sku) (seed_products ())

let contains_ci haystack needle =
  let lower s = String.lowercase_ascii s in
  let h = lower haystack and n = lower needle in
  let hl = String.length h and nl = String.length n in
  if nl = 0 then true
  else
    let rec loop i = i + nl <= hl && (String.sub h i nl = n || loop (i + 1)) in
    loop 0

let search query =
  List.filter (fun p -> contains_ci p.name query || contains_ci p.description query) (seed_products ())
