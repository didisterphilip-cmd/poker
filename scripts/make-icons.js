// Generates PWA PNG icons (no external deps, uses built-in zlib).
// Draws a simple catering icon: rounded brand-colored tile with a white plate,
// a fork and a knife. Outputs 192x192, 512x512, and a 512x512 maskable icon.
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

const BRAND = [15, 118, 110];     // teal-700
const BRAND_DARK = [12, 74, 71];  // darker teal for maskable bg
const PLATE = [248, 250, 249];    // near-white
const CUTLERY = [30, 41, 59];     // slate-800

function makeCanvas(size) {
  return { size, data: new Uint8Array(size * size * 4) };
}
function setPx(c, x, y, [r, g, b], a = 255) {
  if (x < 0 || y < 0 || x >= c.size || y >= c.size) return;
  const i = (y * c.size + x) * 4;
  c.data[i] = r; c.data[i + 1] = g; c.data[i + 2] = b; c.data[i + 3] = a;
}
function fillBg(c, color) {
  for (let y = 0; y < c.size; y++) for (let x = 0; x < c.size; x++) setPx(c, x, y, color);
}
function fillRoundedRect(c, x0, y0, w, h, radius, color) {
  for (let y = y0; y < y0 + h; y++) {
    for (let x = x0; x < x0 + w; x++) {
      let inside = true;
      const rx = Math.min(x - x0, x0 + w - 1 - x);
      const ry = Math.min(y - y0, y0 + h - 1 - y);
      if (rx < radius && ry < radius) {
        const dx = radius - rx, dy = radius - ry;
        if (dx * dx + dy * dy > radius * radius) inside = false;
      }
      if (inside) setPx(c, x, y, color);
    }
  }
}
function fillCircle(c, cx, cy, r, color) {
  for (let y = cy - r; y <= cy + r; y++) {
    for (let x = cx - r; x <= cx + r; x++) {
      const dx = x - cx, dy = y - cy;
      if (dx * dx + dy * dy <= r * r) setPx(c, x, y, color);
    }
  }
}
function ring(c, cx, cy, rOuter, rInner, color) {
  for (let y = cy - rOuter; y <= cy + rOuter; y++) {
    for (let x = cx - rOuter; x <= cx + rOuter; x++) {
      const dx = x - cx, dy = y - cy, d2 = dx * dx + dy * dy;
      if (d2 <= rOuter * rOuter && d2 >= rInner * rInner) setPx(c, x, y, color);
    }
  }
}

function drawIcon(size, { bgColor, inset }) {
  const c = makeCanvas(size);
  const s = size;
  fillBg(c, bgColor);
  // rounded tile (for non-maskable this is the visible surface)
  const pad = Math.round(s * inset);
  fillRoundedRect(c, pad, pad, s - 2 * pad, s - 2 * pad, Math.round(s * 0.16), BRAND);

  const cx = Math.round(s * 0.55);
  const cy = Math.round(s * 0.5);
  const plateR = Math.round(s * 0.20);
  // plate
  fillCircle(c, cx, cy, plateR, PLATE);
  ring(c, cx, cy, plateR, plateR - Math.round(s * 0.02), [225, 231, 229]);

  // fork (left of plate)
  const forkX = Math.round(s * 0.24);
  const forkTop = Math.round(s * 0.30);
  const forkBottom = Math.round(s * 0.70);
  const tineW = Math.round(s * 0.018);
  const gap = Math.round(s * 0.03);
  const forkHeadBottom = Math.round(s * 0.44);
  for (let k = -1; k <= 1; k++) {
    fillRoundedRect(c, forkX + k * gap - Math.round(tineW / 2), forkTop, tineW, forkHeadBottom - forkTop, Math.round(tineW / 2), CUTLERY);
  }
  // fork neck + handle
  const neckW = Math.round(s * 0.05);
  fillRoundedRect(c, forkX - Math.round(neckW / 2), forkHeadBottom - Math.round(s * 0.01), neckW, forkBottom - forkHeadBottom, Math.round(neckW / 2), CUTLERY);

  // knife (right of plate)
  const knifeX = Math.round(s * 0.83);
  const bladeW = Math.round(s * 0.055);
  fillRoundedRect(c, knifeX - Math.round(bladeW / 2), forkTop, bladeW, forkBottom - forkTop, Math.round(bladeW / 2), CUTLERY);

  return c;
}

function encodePNG(canvas) {
  const { size, data } = canvas;
  const bytesPerPixel = 4;
  const stride = size * bytesPerPixel;
  const raw = Buffer.alloc((stride + 1) * size);
  for (let y = 0; y < size; y++) {
    raw[y * (stride + 1)] = 0; // no filter
    data.subarray(y * stride, (y + 1) * stride).forEach((v, i) => {
      raw[y * (stride + 1) + 1 + i] = v;
    });
  }
  const compressed = zlib.deflateSync(raw, { level: 9 });

  function chunk(type, dataBuf) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(dataBuf.length, 0);
    const typeBuf = Buffer.from(type, 'ascii');
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(Buffer.concat([typeBuf, dataBuf])) >>> 0, 0);
    return Buffer.concat([len, typeBuf, dataBuf, crc]);
  }
  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8;   // bit depth
  ihdr[9] = 6;   // color type RGBA
  ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  return Buffer.concat([sig, chunk('IHDR', ihdr), chunk('IDAT', compressed), chunk('IEND', Buffer.alloc(0))]);
}

const crcTable = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();
function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = crcTable[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

const outDir = path.join(__dirname, '..', 'docs', 'icons');
fs.mkdirSync(outDir, { recursive: true });

const specs = [
  { name: 'icon-192.png', size: 192, bgColor: BRAND, inset: 0 },
  { name: 'icon-512.png', size: 512, bgColor: BRAND, inset: 0 },
  { name: 'icon-maskable-512.png', size: 512, bgColor: BRAND_DARK, inset: 0.12 },
];
for (const spec of specs) {
  const c = drawIcon(spec.size, spec);
  fs.writeFileSync(path.join(outDir, spec.name), encodePNG(c));
  console.log('wrote', spec.name);
}
console.log('done');
