(* ONLY interface other modules/apps may import from the catalog module. *)
type product = Catalog_internal.Catalog_service.product = {
  sku : string;
  name : string;
  description : string;
  price_cents : int;
  category : string;
  stock_quantity : int;
}

let list ?category () = Catalog_internal.Catalog_service.list ?category ()
let get sku = Catalog_internal.Catalog_service.get sku
let search query = Catalog_internal.Catalog_service.search query

let to_yojson (p : product) : Yojson.Safe.t =
  `Assoc [
    ("sku", `String p.sku);
    ("name", `String p.name);
    ("description", `String p.description);
    ("price_cents", `Int p.price_cents);
    ("category", `String p.category);
    ("stock_quantity", `Int p.stock_quantity);
  ]
