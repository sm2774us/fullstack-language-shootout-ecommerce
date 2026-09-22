(* Business logic — hidden from the outside world. *)
type order = {
  order_id : string;
  total_cents : int;
  mutable status : string;
  mutable payment_id : string option;
  mutable cancel_reason : string option;
  mutable refunded_cents : int option;
}

let store : (string, order) Hashtbl.t = Hashtbl.create 16

let create_order order_id total_cents =
  let o = { order_id; total_cents; status = "PENDING"; payment_id = None; cancel_reason = None; refunded_cents = None } in
  Hashtbl.replace store order_id o;
  o

let get_order order_id = Hashtbl.find_opt store order_id

(* Transitions PENDING -> PAID. Called only after Stripe signature verification. *)
let mark_paid order_id payment_id =
  match Hashtbl.find_opt store order_id with
  | None -> None
  | Some o -> o.status <- "PAID"; o.payment_id <- Some payment_id; Some o

let mark_failed order_id reason =
  match Hashtbl.find_opt store order_id with
  | None -> None
  | Some o -> o.status <- "CANCELLED"; o.cancel_reason <- Some reason; Some o

let mark_refunded order_id amount_cents =
  match Hashtbl.find_opt store order_id with
  | Some o when o.status = "PAID" -> o.status <- "REFUNDED"; o.refunded_cents <- Some amount_cents; Some o
  | _ -> None
