(* ONLY interface other modules/apps may import from the orders module. *)
let create_order = Orders_internal.Order_service.create_order
let get_order = Orders_internal.Order_service.get_order
let mark_paid = Orders_internal.Order_service.mark_paid
let mark_failed = Orders_internal.Order_service.mark_failed
let mark_refunded = Orders_internal.Order_service.mark_refunded

let to_yojson (o : Orders_internal.Order_service.order) : Yojson.Safe.t =
  let open Orders_internal.Order_service in
  `Assoc ([
    ("order_id", `String o.order_id);
    ("total_cents", `Int o.total_cents);
    ("status", `String o.status);
  ] @ (match o.payment_id with Some p -> [("payment_id", `String p)] | None -> [])
    @ (match o.cancel_reason with Some r -> [("cancel_reason", `String r)] | None -> [])
    @ (match o.refunded_cents with Some c -> [("refunded_cents", `Int c)] | None -> []))
