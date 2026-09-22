(* ONLY interface other modules/apps may import from the payments module. *)
let authorize = Payments_internal.Payment_service.authorize
let capture = Payments_internal.Payment_service.capture
let refund = Payments_internal.Payment_service.refund
