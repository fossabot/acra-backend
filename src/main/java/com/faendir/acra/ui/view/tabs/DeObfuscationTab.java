package com.faendir.acra.ui.view.tabs;

import com.faendir.acra.security.SecurityUtils;
import com.faendir.acra.sql.data.ProguardMappingRepository;
import com.faendir.acra.sql.model.App;
import com.faendir.acra.sql.model.Permission;
import com.faendir.acra.sql.model.ProguardMapping;
import com.faendir.acra.ui.NavigationManager;
import com.faendir.acra.ui.view.base.InMemoryUpload;
import com.faendir.acra.ui.view.base.MyGrid;
import com.faendir.acra.ui.view.base.MyTabSheet;
import com.faendir.acra.ui.view.base.Popup;
import com.faendir.acra.ui.view.base.ValidatedField;
import com.faendir.acra.util.BufferedDataProvider;
import com.faendir.acra.util.Style;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;
import org.springframework.lang.NonNull;
import org.vaadin.risto.stepper.IntStepper;

/**
 * @author Lukas
 * @since 19.05.2017
 */
@SpringComponent
@ViewScope
public class DeObfuscationTab implements MyTabSheet.Tab {
    public static final String CAPTION = "De-Obfuscation";
    @NonNull private final ProguardMappingRepository mappingRepository;
    @NonNull private final BufferedDataProvider.Factory factory;

    public DeObfuscationTab(@NonNull ProguardMappingRepository mappingRepository, @NonNull BufferedDataProvider.Factory factory) {
        this.mappingRepository = mappingRepository;
        this.factory = factory;
    }

    @Override
    public Component createContent(@NonNull App app, @NonNull NavigationManager navigationManager) {
        VerticalLayout layout = new VerticalLayout();
        MyGrid<ProguardMapping> grid = new MyGrid<>(null, factory.create(app, mappingRepository::findAllByApp, mappingRepository::countAllByApp));
        grid.addColumn(ProguardMapping::getVersionCode, "Version");
        grid.setWidth(100, VerticalLayout.Unit.PERCENTAGE);
        layout.addComponent(grid);
        layout.setSizeFull();
        Style.NO_PADDING.apply(layout);
        if (SecurityUtils.hasPermission(app, Permission.Level.EDIT)) {
            layout.addComponent(new Button("Add File", e -> {
                IntStepper version = new IntStepper("Version code");
                version.setValue(1);
                InMemoryUpload upload = new InMemoryUpload("Mapping file:");
                ProgressBar progressBar = new ProgressBar();
                upload.addProgressListener((readBytes, contentLength) -> layout.getUI().access(() -> progressBar.setValue((float) readBytes / contentLength)));
                new Popup().setTitle("New Mapping Configuration")
                        .addComponent(version)
                        .addValidatedField(ValidatedField.of(upload, () -> upload, consumer -> upload.addFinishedListener(event -> consumer.accept(upload)))
                                .addValidator(InMemoryUpload::isUploaded, "Upload failed"))
                        .addComponent(progressBar)
                        .addCreateButton(popup -> {
                            mappingRepository.save(new ProguardMapping(app, version.getValue(), upload.getUploadedString()));
                            grid.getDataProvider().refreshAll();
                            popup.close();
                        })
                        .show();
            }));
        }
        layout.setExpandRatio(grid, 1);
        return layout;
    }

    @Override
    public String getCaption() {
        return CAPTION;
    }
}
