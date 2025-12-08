import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/combo-box/src/vaadin-combo-box.js';
import 'Frontend/generated/jar-resources/comboBoxConnector.js';
import 'Frontend/generated/jar-resources/vaadin-grid-flow-selection-column.js';
import '@vaadin/radio-group/src/vaadin-radio-group.js';
import '@vaadin/radio-group/src/vaadin-radio-button.js';
import '@vaadin/app-layout/src/vaadin-app-layout.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import '@vaadin/tabs/src/vaadin-tab.js';
import '@vaadin/icon/src/vaadin-icon.js';
import '@vaadin/upload/src/vaadin-upload.js';
import '@vaadin/context-menu/src/vaadin-context-menu.js';
import 'Frontend/generated/jar-resources/contextMenuConnector.js';
import 'Frontend/generated/jar-resources/contextMenuTargetConnector.js';
import '@vaadin/form-layout/src/vaadin-form-item.js';
import '@vaadin/multi-select-combo-box/src/vaadin-multi-select-combo-box.js';
import '@vaadin/grid/src/vaadin-grid.js';
import '@vaadin/grid/src/vaadin-grid-column.js';
import '@vaadin/grid/src/vaadin-grid-sorter.js';
import '@vaadin/checkbox/src/vaadin-checkbox.js';
import 'Frontend/generated/jar-resources/gridConnector.ts';
import '@vaadin/button/src/vaadin-button.js';
import 'Frontend/generated/jar-resources/buttonFunctions.js';
import '@vaadin/split-layout/src/vaadin-split-layout.js';
import '@vaadin/text-field/src/vaadin-text-field.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/form-layout/src/vaadin-form-layout.js';
import '@vaadin/dialog/src/vaadin-dialog.js';
import '@vaadin/text-area/src/vaadin-text-area.js';
import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/app-layout/src/vaadin-drawer-toggle.js';
import '@vaadin/horizontal-layout/src/vaadin-horizontal-layout.js';
import '@vaadin/tabs/src/vaadin-tabs.js';
import '@vaadin/grid/src/vaadin-grid-column-group.js';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import '@vaadin/confirm-dialog/src/vaadin-confirm-dialog.js';
import '@vaadin/notification/src/vaadin-notification.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/vaadin-lumo-styles/color-global.js';
import '@vaadin/vaadin-lumo-styles/typography-global.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '3935dab22c25b23585004b875f98c5f12108c4fd884f54948805d98bdfbe826a') {
    pending.push(import('./chunks/chunk-8bbba42e44e31eb63b63df2b661370f404115655427ad47234f9e38d51dbdc8f.js'));
  }
  if (key === '44182c424dae29470250f8fc64536da4f6ad714cacb15debab41069bc8280749') {
    pending.push(import('./chunks/chunk-6591b3cea910f7d9d8c065b6922ec84eacabb7c5fe7f226b32b3c86e013855eb.js'));
  }
  if (key === '0988e3efaa82ff7f047f0fdbad9b10d9f1b668194405e64d00807aba54be8502') {
    pending.push(import('./chunks/chunk-bd228ba326adf31b51ef0499b79ce28a5c4d5012d20b969fd3bd1be99897b327.js'));
  }
  if (key === '00b227d0b3f8210295cdfc3b55a8177e4f75e2ecb99641bdd805253b284b1298') {
    pending.push(import('./chunks/chunk-8fb3f7746fe5429b7810795854d4cff271127f391d608bd7cedc68ca3609397a.js'));
  }
  if (key === '988e37c5234ee65e48ee24b76501c178dc8b90104a377624452823f6c170cdc4') {
    pending.push(import('./chunks/chunk-677231ec51ead9b842c2b1b978ad3fa410f71559f927206b8bc685e8a5873695.js'));
  }
  if (key === '182029be1873e5473cdfac598de050b8dec8b4459bd35d1878567f0412526a9a') {
    pending.push(import('./chunks/chunk-b21a8618a32ffe85b6e0fc11334ccfeeab8143d63202ed4ab241a26aa0b6743e.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}