-- Prueba de integración del contrato con Unity (sprint 4).
--
-- Simula, como el jugador demo autenticado, las llamadas que hace Unity:
--   claim_pending_rewards -> save_game_progress -> adjust_coins (+, -, inválidos) -> sin sesión.
-- El inicio de sesión con contraseña se prueba aparte con unity_http_test.ps1.
--
-- No deja datos alterados: todo corre dentro de un bloque con manejador de excepción
-- (subtransacción) que siempre termina lanzando OD000, así que Postgres deshace cada cambio
-- (saldo, movimientos de monedas, stats, recompensas entregadas) aunque el cliente use autocommit.
-- Si una comprobación falla se lanza otro error y la consulta entera falla con su mensaje.
--
-- Uso: pegar en el SQL Editor de Supabase (o psql) y ejecutar. Devuelve un jsonb con el reporte.
-- Para otra cuenta, cambiar v_player.

create or replace function pg_temp.odisea_unity_contract_test() returns jsonb
language plpgsql as $$
declare
  v_player constant uuid := '104e3743-e9ec-4ab4-a212-043591ca10c1'; -- cuenta demo
  v_report jsonb := '[]'::jsonb;
  v_before jsonb; v_after jsonb; v_res jsonb;
  v_coins bigint; v_claimed bigint; v_err text;
begin
  begin
    -- Lo mismo que hace PostgREST con el JWT de Unity.
    perform set_config('request.jwt.claims', json_build_object('sub', v_player, 'role', 'authenticated')::text, true);
    perform set_config('role', 'authenticated', true);

    v_before := public.get_player_snapshot();
    if (v_before->'player'->>'id')::uuid is distinct from v_player then
      raise exception 'FALLO snapshot: no es el jugador demo';
    end if;
    v_coins := (v_before->>'coins')::bigint;
    v_report := v_report || jsonb_build_object('paso', 'snapshot inicial', 'monedas', v_coins,
      'pendientes', v_before->'pending_rewards');

    -- 1) Recompensas pendientes: se suman al saldo y el contador queda en 0.
    v_res := public.claim_pending_rewards();
    v_claimed := coalesce((v_res->>'coins')::bigint, 0);
    v_after := public.get_player_snapshot();
    if (v_after->>'coins')::bigint <> v_coins + v_claimed then
      raise exception 'FALLO claim: saldo % esperado %', v_after->>'coins', v_coins + v_claimed;
    end if;
    if coalesce((v_after->>'pending_rewards')::int, 0) <> 0 then
      raise exception 'FALLO claim: siguen % pendientes', v_after->>'pending_rewards';
    end if;
    v_coins := v_coins + v_claimed;
    v_report := v_report || jsonb_build_object('paso', 'claim_pending_rewards', 'ok', true, 'respuesta', v_res);

    -- 2) Guardado de partida: +10 XP y +60 s de juego; la vida se recorta al máximo.
    v_res := public.save_game_progress(jsonb_build_object('stats', jsonb_build_object(
      'level', (v_before->'stats'->>'level')::int,
      'xp', (v_before->'stats'->>'xp')::int + 10,
      'health', 99999,
      'health_max', (v_before->'stats'->>'health_max')::int,
      -- Unity debe mandar siempre los máximos: si faltan, la función los regresa a 100.
      'resistance_max', (v_before->'stats'->>'resistance_max')::int,
      'playtime_seconds', (v_before->'stats'->>'playtime_seconds')::bigint + 60)));
    v_after := public.get_player_snapshot();
    if (v_after->'stats'->>'xp')::int <> (v_before->'stats'->>'xp')::int + 10
       or (v_after->'stats'->>'playtime_seconds')::bigint <> (v_before->'stats'->>'playtime_seconds')::bigint + 60 then
      raise exception 'FALLO save_game_progress: stats %', v_after->'stats';
    end if;
    if (v_after->'stats'->>'health')::int <> (v_after->'stats'->>'health_max')::int then
      raise exception 'FALLO save_game_progress: la vida no se recortó al máximo';
    end if;
    v_report := v_report || jsonb_build_object('paso', 'save_game_progress', 'ok', true, 'respuesta', v_res);

    -- 3) Monedas: ganar y gastar.
    v_res := public.adjust_coins(50, 'prueba integración: ganar');
    if (v_res->>'coins')::bigint <> v_coins + 50 then raise exception 'FALLO adjust_coins(+50): %', v_res; end if;
    v_res := public.adjust_coins(-50, 'prueba integración: gastar');
    if (v_res->>'coins')::bigint <> v_coins then raise exception 'FALLO adjust_coins(-50): %', v_res; end if;
    v_report := v_report || jsonb_build_object('paso', 'adjust_coins +50/-50', 'ok', true);

    -- 4) Montos rechazados: 0, más de 1000 y gastar más de lo que hay.
    foreach v_claimed in array array[0, 1001, -(least(v_coins + 1, 1000))] loop
      v_err := null;
      begin
        if v_claimed < 0 and v_coins + 1 > 1000 then
          v_err := 'omitida (saldo >= 1000)';
        else
          perform public.adjust_coins(v_claimed, 'prueba integración: inválido');
        end if;
      exception when others then v_err := sqlerrm;
      end;
      if v_err is null then raise exception 'FALLO adjust_coins(%) debió rechazarse', v_claimed; end if;
      v_report := v_report || jsonb_build_object('paso', format('adjust_coins(%s) rechazado', v_claimed), 'mensaje', v_err);
    end loop;

    -- 5) Sin sesión: todas las funciones del contrato deben negarse.
    perform set_config('request.jwt.claims', '{"role":"authenticated"}', true);
    v_err := null;
    begin
      perform public.adjust_coins(1, 'prueba integración: sin sesión');
    exception when others then v_err := sqlerrm;
    end;
    if v_err is null then raise exception 'FALLO adjust_coins sin sesión fue aceptado'; end if;
    v_report := v_report || jsonb_build_object('paso', 'adjust_coins sin sesión', 'mensaje', v_err);

    -- Deshacer todo lo anterior.
    raise exception using errcode = 'OD000', message = v_report::text;
  exception when sqlstate 'OD000' then
    return jsonb_build_object('resultado', 'OK, todo deshecho', 'pasos', sqlerrm::jsonb);
  end;
end $$;

select pg_temp.odisea_unity_contract_test() as reporte;
