(* Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
   tolerance), using OCaml's digestif for constant-time HMAC — mirrors
   backend-python's webhook_handler.py. *)
let tolerance_seconds = 300.

let split_header sig_header =
  let parts = String.split_on_char ',' sig_header in
  List.fold_left
    (fun (ts, sigs) part ->
      match String.index_opt part '=' with
      | None -> (ts, sigs)
      | Some i ->
        let key = String.trim (String.sub part 0 i) in
        let value = String.trim (String.sub part (i + 1) (String.length part - i - 1)) in
        if key = "t" then (float_of_string_opt value, sigs)
        else if key = "v1" then (ts, value :: sigs)
        else (ts, sigs))
    (None, []) parts

let verify ~payload ~sig_header ~secret =
  if secret = "" then Error "STRIPE_WEBHOOK_SECRET is not configured"
  else
    let timestamp, sigs = split_header sig_header in
    match timestamp, sigs with
    | None, _ | _, [] -> Error "malformed Stripe-Signature header"
    | Some ts, sigs ->
      let now = Unix.gettimeofday () in
      if Float.abs (now -. ts) > tolerance_seconds then
        Error "timestamp outside tolerance — possible replay"
      else
        let signed_payload = Printf.sprintf "%.0f.%s" ts payload in
        let expected =
          Digestif.SHA256.hmac_string ~key:secret signed_payload
          |> Digestif.SHA256.to_hex
        in
        if List.exists (fun s -> String.equal s expected) sigs then Ok ()
        else Error "signature mismatch"
