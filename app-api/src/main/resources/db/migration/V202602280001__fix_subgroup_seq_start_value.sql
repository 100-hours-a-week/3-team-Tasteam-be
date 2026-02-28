-- subgroup_seq는 START=1, INCREMENT=50 으로 생성되어 있어
-- Hibernate pooled 옵티마이저가 첫 nextval(1)을 풀의 상한으로 해석,
-- 첫 번째 풀 범위가 [-48, 1]이 되어 음수 ID가 발생할 수 있다.
-- allocationSize=50 기준으로 첫 풀이 [1,50]이 되려면 nextval >= 50 이어야 한다.
-- 시퀀스가 아직 50 미만이면 50으로 이동하여 문제를 방지한다.
DO $$
BEGIN
    IF (SELECT last_value FROM subgroup_seq) < 50 THEN
        -- false: is_called=false 이므로 다음 nextval이 정확히 50을 반환함
        PERFORM setval('subgroup_seq', 50, false);
    END IF;
END $$;
