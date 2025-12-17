// Shape configurations mapping
// Each shape has its own model ID, metal, and band configuration

export const SHAPE_CONFIGS = {
  Fiston: {
    shape: 'Fiston',
    modelId: 'DfRULQ-OSk6TjbYAcB9zkA',
    metal: 'Gold 24k',
    band: 'Single band'
  },
  Myfav: {
    shape: 'Myfav',
    modelId: 'QAauSV24QiuM5CxA_1797w',
    metal: 'Gold 24k',
    band: 'Single band'
  },
  Oval: {
    shape: 'Oval',
    modelId: 'N1w9lJ3FQfOWsrC7jeeYfA',
    metal: 'Gold 24k',
    band: 'Single band'
  },
  Trilogy: {
    shape: 'Trilogy',
    modelId: 'FWV7-qA6QEG_Ju8pjSItuA',
    metal: 'Gold 24k',
    band: 'Single band'
  },
  Heart: {
    shape: 'Heart',
    modelId: 'Qteju98xRgKe8y5KylzXIw',
    metal: 'Gold 24k',
    band: 'Single band'
  },
  Pear: {
    shape: 'Pear',
    modelId: 'MKyTIlEyRbi89oT6bH76yA',
    metal: 'Silver',
    band: 'Single band'
  },
  Twin: {
    shape: 'Twin',
    modelId: 'dY4BIhDDQNmCVTRrEpV2QQ',
    metal: 'Silver',
    band: 'Single band'
  },
  New: {
    shape: 'New',
    modelId: 'R4Yyjh0QQlmEtazcWf7IGA',
    metal: 'Silver',
    band: 'Single band'
  },
  Flower: {
    shape: 'Flower',
    modelId: 'dGVcGf5ZSwq20ULDHOeaNQ',
    metal: 'Silver',
    band: 'Single band'
  },
  Example: {
    shape: 'Example',
    modelId: 'RUsrBi-vQey2vExitZOYig',
    metal: 'Gold 24k',
    band: 'Single band'
  },
  Lumex91Cadillac: {
    shape: 'Lumex91Cadillac',
    modelId: 'VdiuGY0xSDOOBoxoHU2y-A',
    metal: 'Gold 24k',
    band: 'Single band'
  },

};

// Get list of available shapes
export const AVAILABLE_SHAPES = Object.keys(SHAPE_CONFIGS);

// Get configuration for a specific shape
export const getShapeConfig = (shapeName) => {
  return SHAPE_CONFIGS[shapeName] || SHAPE_CONFIGS.New;
};
