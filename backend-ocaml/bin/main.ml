(* Single deployable entry point for the OCaml backend. *)
let perf_json () =
  let now_ms = Unix.gettimeofday () *. 1000. in
  `Assoc [
    ("backend", `String "backend-ocaml");
    ("language", `String "ocaml");
    ("server_time_unix_ms", `Float now_ms);
    ("status", `String "ok");
  ] |> Yojson.Safe.to_string

let products_handler request =
  let category = Dream.query request "category" in
  let products = Catalog_public_api.list ?category () in
  Dream.json (Yojson.Safe.to_string (`List (List.map Catalog_public_api.to_yojson products)))

let search_handler request =
  let q = Option.value (Dream.query request "q") ~default:"" in
  let products = Catalog_public_api.search q in
  Dream.json (Yojson.Safe.to_string (`List (List.map Catalog_public_api.to_yojson products)))

let product_handler request =
  let sku = Dream.param request "sku" in
  match Catalog_public_api.get sku with
  | Some p -> Dream.json (Yojson.Safe.to_string (Catalog_public_api.to_yojson p))
  | None -> Dream.json {|{"error":"not found"}|}

let stripe_webhook_handler request =
  let%lwt payload = Dream.body request in
  let sig_header = Option.value (Dream.header request "Stripe-Signature") ~default:"" in
  let secret = Option.value (Sys.getenv_opt "STRIPE_WEBHOOK_SECRET") ~default:"" in
  match Payments_internal.Stripe_webhook_verifier.verify ~payload ~sig_header ~secret with
  | Error err -> Dream.json (Printf.sprintf {|{"error":"%s"}|} err)
  | Ok () ->
    let event = Yojson.Safe.from_string payload in
    let open Yojson.Safe.Util in
    let event_type = try event |> member "type" |> to_string with _ -> "" in
    let data_obj = try event |> member "data" |> member "object" with _ -> `Null in
    let order_id = try data_obj |> member "metadata" |> member "order_id" |> to_string with _ -> "" in
    let payment_intent_id = try data_obj |> member "id" |> to_string with _ -> "" in

    (match event_type with
     | "payment_intent.succeeded" when order_id <> "" ->
       ignore (Payments_public_api.capture payment_intent_id);
       ignore (Orders_public_api.mark_paid order_id payment_intent_id)
     | "payment_intent.payment_failed" when order_id <> "" ->
       let reason =
         try data_obj |> member "last_payment_error" |> member "message" |> to_string
         with _ -> "payment failed"
       in
       ignore (Orders_public_api.mark_failed order_id reason)
     | "charge.refunded" when order_id <> "" ->
       let amount = try data_obj |> member "amount_refunded" |> to_int with _ -> 0 in
       ignore (Orders_public_api.mark_refunded order_id amount)
     | _ -> ());

    Dream.json (Printf.sprintf {|{"received":true,"event_type":"%s"}|} event_type)

let () =
  Dream.run ~interface:"0.0.0.0" ~port:8080
  @@ Dream.logger
  @@ Dream.router [
       Dream.get "/api/perf" (fun _ -> Dream.json (perf_json ()));
       Dream.get "/healthz" (fun _ -> Dream.json {|{"status":"ok"}|});
       Dream.get "/api/products" products_handler;
       Dream.get "/api/products/search" search_handler;
       Dream.get "/api/products/:sku" product_handler;
       Dream.post "/webhooks/stripe" stripe_webhook_handler;
     ]
