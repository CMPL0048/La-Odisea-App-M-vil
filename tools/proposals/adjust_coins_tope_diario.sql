-- PROPUESTA (sprint 4) — NO APLICADA. Cambia el contrato con Unity: aplicar solo con visto bueno.
--
-- Hoy public.adjust_coins acepta hasta +1000 por llamada y sin límite de llamadas.
-- Propuesta: que Unity pueda sumar como máximo 2000 monedas por jugador por día
-- (día de Nayarit, America/Mazatlan). Gastar (montos negativos) no tiene tope.
--
-- Cambios:
--   1. Nuevo motivo 'game_reward' en coin_transactions para distinguir lo que suma Unity
--      (hoy usa 'other', igual que claim_pending_rewards, y no se puede separar).
--   2. adjust_coins bloquea la cartera del jugador, suma lo ganado hoy con 'game_reward' y,
--      si el nuevo monto pasa de 2000, rechaza la llamada completa (no la recorta) con el mensaje
--      'Tope diario de monedas alcanzado (quedan N hoy)'.
--   Recompensas de minijuegos ('minigame_reward'), pendientes ('other') y descuentos no cuentan.
--
-- Lo que Unity tendría que hacer: tratar ese error como «no se dio la recompensa» (no reintentar
-- en bucle). La firma y la respuesta {"coins": saldo} no cambian.

alter table public.coin_transactions drop constraint coin_transactions_reason_check;
alter table public.coin_transactions add constraint coin_transactions_reason_check check (reason = any (array[
  'minigame_reward', 'negotiation_discount', 'manual_adjustment', 'purchase', 'other', 'game_reward']));

create or replace function public.adjust_coins(p_amount bigint, p_reason text default null)
 returns jsonb
 language plpgsql
 security definer
 set search_path to ''
as $function$
declare
  c_daily_cap constant bigint := 2000;
  v_uid uuid := (select auth.uid()); v_balance bigint; v_today bigint;
begin
  if v_uid is null then raise exception 'No autenticado'; end if;
  if p_amount is null or p_amount = 0 or abs(p_amount) > 1000 then raise exception 'Monto inválido (1 a 1000)'; end if;
  -- Bloquear la cartera evita que dos llamadas simultáneas se salten el tope.
  perform 1 from public.player_wallet where player_id = v_uid for update;
  if p_amount > 0 then
    select coalesce(sum(amount), 0) into v_today from public.coin_transactions
     where player_id = v_uid and reason = 'game_reward'
       and created_at >= (date_trunc('day', now() at time zone 'America/Mazatlan') at time zone 'America/Mazatlan');
    if v_today + p_amount > c_daily_cap then
      raise exception 'Tope diario de monedas alcanzado (quedan % hoy)', greatest(c_daily_cap - v_today, 0);
    end if;
  end if;
  update public.player_wallet set coins_balance = coins_balance + p_amount, updated_at = now()
   where player_id = v_uid and coins_balance + p_amount >= 0
   returning coins_balance into v_balance;
  if not found then raise exception 'Monedas insuficientes'; end if;
  insert into public.coin_transactions (player_id, amount, reason, source_ref)
    values (v_uid, p_amount, case when p_amount > 0 then 'game_reward' else 'purchase' end, left(p_reason, 100));
  return jsonb_build_object('coins', v_balance);
end $function$;
