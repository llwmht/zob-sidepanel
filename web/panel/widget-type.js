// A widget type names a folder under web/widgets/. Checked before any import so a layout
// cannot load a module from elsewhere.
const TYPE = /^[a-z][a-z0-9-]{0,31}$/;

export function isValidWidgetType(type) {
  return typeof type === 'string' && TYPE.test(type);
}
