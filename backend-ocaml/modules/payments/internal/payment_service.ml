(* Business logic — hidden from the outside world. *)
type payment = { payment_id : string; order_id : string; mutable status : string; amount_cents : int }

let store : (string, payment) Hashtbl.t = Hashtbl.create 16

let authorize payment_id order_id amount_cents =
  let p = { payment_id; order_id; status = "AUTHORIZED"; amount_cents } in
  Hashtbl.replace store payment_id p;
  p

let capture payment_id =
  match Hashtbl.find_opt store payment_id with
  | Some p when p.status = "AUTHORIZED" -> p.status <- "CAPTURED"; Some p
  | _ -> None

let refund payment_id =
  match Hashtbl.find_opt store payment_id with
  | Some p when p.status = "CAPTURED" -> p.status <- "REFUNDED"; Some p
  | _ -> None
