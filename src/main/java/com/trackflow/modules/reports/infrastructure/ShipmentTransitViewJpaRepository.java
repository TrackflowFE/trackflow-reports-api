package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.domain.ShipmentTransitView;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentTransitViewJpaRepository extends JpaRepository<ShipmentTransitView, String> {

    /** Fila agregada: un tramo (o el total), su promedio en segundos y los envíos que lo componen. */
    interface TramoPromedio {
        String getTramo();

        Double getPromedio();

        Long getEnvios();
    }

    @Query("""
            select count(v) from ShipmentTransitView v
            where v.deliveredAt >= :desde and v.deliveredAt < :hasta
            """)
    long contarEntregados(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /**
     * avg() y count(expr) ignoran los nulos: un tramo promedia solo los envíos que
     * tienen sus dos hitos. Los códigos deben coincidir con TramoDeTransito.codigo().
     */
    @Query(value = """
            with entregados as (
                select * from reports_shipment_transit
                where delivered_at >= :desde and delivered_at < :hasta
            )
            select 'TOTAL' as tramo,
                   cast(avg(extract(epoch from (delivered_at - received_at))) as double precision) as promedio,
                   count(delivered_at - received_at) as envios
            from entregados
            union all
            select 'ORIGIN_PROCESSING',
                   cast(avg(extract(epoch from (dispatched_at - received_at))) as double precision),
                   count(dispatched_at - received_at)
            from entregados
            union all
            select 'TRANSPORT',
                   cast(avg(extract(epoch from (arrived_at - dispatched_at))) as double precision),
                   count(arrived_at - dispatched_at)
            from entregados
            union all
            select 'DESTINATION_PROCESSING',
                   cast(avg(extract(epoch from (out_for_delivery_at - arrived_at))) as double precision),
                   count(out_for_delivery_at - arrived_at)
            from entregados
            union all
            select 'LAST_MILE',
                   cast(avg(extract(epoch from (delivered_at - out_for_delivery_at))) as double precision),
                   count(delivered_at - out_for_delivery_at)
            from entregados
            """, nativeQuery = true)
    List<TramoPromedio> promediarEntregados(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
