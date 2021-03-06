package com.faendir.acra.ui.view;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import com.faendir.acra.sql.data.AttachmentRepository;
import com.faendir.acra.sql.data.ProguardMappingRepository;
import com.faendir.acra.sql.data.ReportRepository;
import com.faendir.acra.sql.model.Attachment;
import com.faendir.acra.sql.model.Permission;
import com.faendir.acra.sql.model.ProguardMapping;
import com.faendir.acra.sql.model.Report;
import com.faendir.acra.ui.annotation.RequiresAppPermission;
import com.faendir.acra.ui.view.base.ParametrizedNamedView;
import com.faendir.acra.util.Style;
import com.faendir.acra.util.Utils;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Lukas
 * @since 13.05.2017
 */
@SpringView(name = "report")
@RequiresAppPermission(Permission.Level.VIEW)
public class ReportView extends ParametrizedNamedView<Report> {
    @NonNull private final ReportRepository reportRepository;
    @NonNull private final AttachmentRepository attachmentRepository;
    @NonNull private final ProguardMappingRepository mappingRepository;

    @Autowired
    public ReportView(@NonNull ReportRepository reportRepository, @NonNull AttachmentRepository attachmentRepository, @NonNull ProguardMappingRepository mappingRepository) {
        super(report -> report.getBug().getApp());
        this.reportRepository = reportRepository;
        this.attachmentRepository = attachmentRepository;
        this.mappingRepository = mappingRepository;
    }

    @Override
    protected void enter(@NonNull Report report) {
        HorizontalLayout attachments = new HorizontalLayout();
        for (Attachment file : attachmentRepository.findAllByReport(report)) {
            Button button = new Button(file.getFilename());
            new FileDownloader(new StreamResource(new ExceptionAwareStreamSource(file.getContent()::getBinaryStream), file.getFilename())).extend(button);
            attachments.addComponent(button);
        }
        Style.apply(attachments, Style.MARGIN_BOTTOM, Style.MARGIN_TOP, Style.MARGIN_LEFT, Style.MARGIN_RIGHT);
        GridLayout summaryGrid = new GridLayout(2, 1);
        summaryGrid.addComponents(new Label("Version", ContentMode.PREFORMATTED), new Label(report.getVersionName(), ContentMode.PREFORMATTED));
        summaryGrid.addComponents(new Label("Email", ContentMode.PREFORMATTED), new Label(report.getUserEmail(), ContentMode.PREFORMATTED));
        summaryGrid.addComponents(new Label("Comment", ContentMode.PREFORMATTED), new Label(report.getUserComment(), ContentMode.PREFORMATTED));
        Optional<ProguardMapping> mapping = mappingRepository.findById(report.getBug().getApp(), report.getVersionCode());
        if (mapping.isPresent()) {
            summaryGrid.addComponents(new Label("De-obfuscated Stacktrace", ContentMode.PREFORMATTED),
                    new Label(Utils.retrace(report.getStacktrace(), mapping.get().getMappings()), ContentMode.PREFORMATTED));
        } else {
            summaryGrid.addComponents(new Label("Stacktrace (No mapping found)", ContentMode.PREFORMATTED), new Label(report.getStacktrace(), ContentMode.PREFORMATTED));
        }
        summaryGrid.addComponents(new Label("Attachments", ContentMode.PREFORMATTED), attachments);
        summaryGrid.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        summaryGrid.setSizeFull();
        Panel summary = new Panel(summaryGrid);
        summary.setCaption("Summary");
        Panel details = new Panel(getLayoutForMap(report.getJsonObject().toMap()));
        details.setCaption("Details");
        VerticalLayout layout = new VerticalLayout(summary, details);
        layout.setSizeUndefined();
        layout.setExpandRatio(details, 1);
        Style.NO_PADDING.apply(layout);
        Panel root = new Panel(layout);
        root.setSizeFull();
        Style.apply(root, Style.NO_BACKGROUND, Style.NO_BORDER);
        setCompositionRoot(root);
        Style.apply(this, Style.PADDING_LEFT, Style.PADDING_RIGHT, Style.PADDING_BOTTOM);
        setSizeFull();
    }

    @NonNull
    private Stream<Component> getLayoutForEntry(@NonNull String key, @NonNull Object value) {
        return Stream.of(new Label(key, ContentMode.PREFORMATTED), getComponentForContent(value));
    }

    @NonNull
    private GridLayout getLayoutForMap(@NonNull Map<String, ?> map) {
        GridLayout layout = new GridLayout(2, 1, map.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .flatMap(entry -> getLayoutForEntry(entry.getKey(), entry.getValue()))
                .toArray(Component[]::new));
        layout.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        layout.setSpacing(false);
        layout.setMargin(false);
        return layout;
    }

    @NonNull
    private Component getComponentForContent(@NonNull Object value) {
        if (value instanceof Map) {
            //noinspection unchecked
            return getLayoutForMap((Map<String, ?>) value);
        } else if (value instanceof List) {
            //noinspection unchecked
            List<Object> values = (List<Object>) value;
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < values.size(); i++) {
                map.put(String.valueOf(i), values.get(0));
            }
            return getLayoutForMap(map);
        }
        return new Label(String.valueOf(value), ContentMode.PREFORMATTED);
    }

    @Override
    public Report validateAndParseFragment(@NonNull String fragment) {
        return reportRepository.findById(fragment).orElse(null);
    }

    private static class ExceptionAwareStreamSource implements StreamResource.StreamSource {
        private final Throwing.Supplier<InputStream> supplier;

        ExceptionAwareStreamSource(Throwing.Supplier<InputStream> supplier) {
            this.supplier = supplier;
        }

        @Override
        public InputStream getStream() {
            return Errors.log().getWithDefault(supplier, null);
        }
    }
}
