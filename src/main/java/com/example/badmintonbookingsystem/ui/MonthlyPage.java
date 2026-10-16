package com.example.badmintonbookingsystem.ui;

import com.example.badmintonbookingsystem.model.User;
import com.example.badmintonbookingsystem.service.Club;
import com.example.badmintonbookingsystem.service.Club.MemberMonth;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** หน้าสรุปประจำเดือน (แอดมิน): สมาชิกแต่ละคนจองกี่ครั้งในเดือนที่เลือก เรียงจากจองมากสุด */
public class MonthlyPage extends Page {

    private final ComboBox<YearMonth> monthBox = new ComboBox<>();
    private final HBox kpis = new HBox(16);
    private final TableView<MemberMonth> table = new TableView<>();

    public MonthlyPage(Club club, User user) {
        super(club, user);

        monthBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth month) {
                return month == null ? "" : Ui.month(month);
            }

            @Override
            public YearMonth fromString(String text) {
                throw new UnsupportedOperationException("เลือกจากรายการเท่านั้น");
            }
        });
        monthBox.valueProperty().addListener(obs -> showMonth());

        table.getColumns().add(Ui.column("อันดับ", row -> table.getItems().indexOf(row) + 1));
        table.getColumns().add(Ui.column("รหัส", row -> row.member().id()));
        table.getColumns().add(Ui.column("สมาชิก", row -> row.member().name()));
        table.getColumns().add(Ui.column("เบอร์โทร", row -> row.member().phone()));
        table.getColumns().add(Ui.column("จำนวนครั้งที่จอง", row -> row.bookings() + " ครั้ง"));
        table.getColumns().add(Ui.column("ชั่วโมงรวม", row -> row.hours() + " ชม."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(Ui.muted("ยังไม่มีสมาชิก"));

        HBox toolbar = new HBox(10, Ui.title("จำนวนการจองของสมาชิกแต่ละคน"), Ui.spacer(), monthBox);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        VBox report = Ui.card(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(report, Priority.ALWAYS);
        getChildren().addAll(kpis, report);
    }

    @Override
    public void refresh() {
        // เดือนที่เลือกได้: ตั้งแต่เดือนแรกที่มีการจอง จนถึงเดือนปัจจุบัน (ใหม่สุดก่อน)
        YearMonth now = YearMonth.now();
        YearMonth first = club.bookings().stream().map(b -> YearMonth.from(b.date()))
                .min(YearMonth::compareTo).orElse(now);
        List<YearMonth> months = new ArrayList<>();
        for (YearMonth m = now; !m.isBefore(first); m = m.minusMonths(1)) months.add(m);

        YearMonth selected = monthBox.getValue();
        monthBox.getItems().setAll(months);
        monthBox.setValue(months.contains(selected) ? selected : now);
        showMonth();
    }

    private void showMonth() {
        YearMonth month = monthBox.getValue();
        if (month == null) return;
        List<MemberMonth> rows = club.monthlySummary(month);
        table.getItems().setAll(rows);

        long total = rows.stream().mapToLong(MemberMonth::bookings).sum();
        long active = rows.stream().filter(r -> r.bookings() > 0).count();
        String top = rows.isEmpty() || rows.getFirst().bookings() == 0 ? "—"
                : rows.getFirst().member().name() + " (" + rows.getFirst().bookings() + " ครั้ง)";
        kpis.getChildren().setAll(
                Ui.kpi(Ui.CALENDAR, "tone-green", "การจองทั้งเดือน", total + " ครั้ง"),
                Ui.kpi(Ui.PEOPLE, "tone-indigo", "สมาชิกที่มาจอง", active + " / " + rows.size() + " คน"),
                Ui.kpi(Ui.CHART, "tone-amber", "จองมากที่สุด", top));
    }
}
