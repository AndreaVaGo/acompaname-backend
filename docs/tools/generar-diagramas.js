// Genera los diagramas Mermaid (clases y entidad-relación) leyendo las entidades JPA del backend.
// Uso:  node docs/tools/generar-diagramas.js [carpeta-con-el-codigo] > docs/diagramas.md
// Por defecto lee src/main/java.

const fs = require("fs");
const path = require("path");

const carpeta = process.argv[2] || "src/main/java";

function buscarArchivos(dir) {
  let resultado = [];
  for (const nombre of fs.readdirSync(dir)) {
    const ruta = path.join(dir, nombre);
    if (fs.statSync(ruta).isDirectory()) {
      resultado = resultado.concat(buscarArchivos(ruta));
    } else if (/Entity\.java$|Estado\w*\.java$/.test(nombre)) {
      resultado.push(ruta);
    }
  }
  return resultado;
}

function quitarComentarios(texto) {
  return texto.replace(/\/\*[\s\S]*?\*\//g, "").replace(/\/\/.*$/gm, "");
}

const entidades = [];
const enums = [];

for (const archivo of buscarArchivos(carpeta)) {
  const texto = quitarComentarios(fs.readFileSync(archivo, "utf8"));

  const enumMatch = texto.match(/enum\s+(\w+)\s*\{([^}]*)\}/);
  if (enumMatch) {
    const valores = enumMatch[2].split(/[,;]/).map((v) => v.trim()).filter(Boolean);
    enums.push({ nombre: enumMatch[1], valores });
    continue;
  }

  const claseMatch = texto.match(/public\s+class\s+(\w+)/);
  if (!claseMatch) continue;

  const campos = [];
  let anotaciones = [];
  for (const linea of texto.split("\n")) {
    const l = linea.trim();
    if (l.startsWith("@")) {
      anotaciones.push(l);
      continue;
    }
    const campo = l.match(/^private\s+([\w<>,\s?]+?)\s+(\w+)\s*(=.*)?;$/);
    if (campo) {
      campos.push({ tipo: campo[1].trim(), nombre: campo[2], anotaciones });
    }
    anotaciones = [];
  }
  if (!entidades.some((e) => e.nombre === claseMatch[1])) {
    entidades.push({ nombre: claseMatch[1], campos });
  }
}

const nombresEntidades = entidades.map((e) => e.nombre);
const nombresEnums = enums.map((e) => e.nombre);

function tipoBase(tipo) {
  const generico = tipo.match(/<\s*(\w+)\s*>/);
  return generico ? generico[1] : tipo;
}

function tipoMermaid(tipo) {
  return tipo.replace(/<(.+)>/, "~$1~");
}

function relacion(campo) {
  const a = campo.anotaciones.join(" ");
  // El lado inverso de una relación (mappedBy) es la misma relación vista desde el otro lado: no se dibuja dos veces
  if (/mappedBy/.test(a)) return "Inversa";
  if (/@ManyToMany/.test(a)) return "ManyToMany";
  if (/@OneToMany/.test(a)) return "OneToMany";
  if (/@ManyToOne/.test(a)) return "ManyToOne";
  if (/@OneToOne/.test(a)) return "OneToOne";
  return null;
}

function nombreColumna(campo) {
  const m = campo.anotaciones.join(" ").match(/@JoinColumn\(\s*name\s*=\s*"(\w+)"/);
  return m ? m[1] : campo.nombre + "_id";
}

function sinEntity(nombre) {
  return nombre.replace(/Entity$/, "");
}

// ---------- Diagrama de clases ----------
const clases = ["```mermaid", "classDiagram"];
for (const e of entidades) {
  clases.push(`    class ${e.nombre} {`);
  for (const c of e.campos) clases.push(`        -${tipoMermaid(c.tipo)} ${c.nombre}`);
  clases.push("    }");
}
for (const en of enums) {
  clases.push(`    class ${en.nombre} {`, "        <<enumeration>>");
  for (const v of en.valores) clases.push(`        ${v}`);
  clases.push("    }");
}
clases.push("");
for (const e of entidades) {
  for (const c of e.campos) {
    const rel = relacion(c);
    const destino = tipoBase(c.tipo);
    if (rel === "ManyToMany") {
      clases.push(`    ${e.nombre} "0..*" -- "0..*" ${destino} : ${c.nombre}`);
    } else if (rel === "ManyToOne") {
      clases.push(`    ${destino} "1" -- "0..*" ${e.nombre} : ${c.nombre}`);
    } else if (rel === "OneToOne") {
      clases.push(`    ${destino} "1" -- "0..1" ${e.nombre} : ${c.nombre}`);
    } else if (rel === "OneToMany") {
      clases.push(`    ${e.nombre} "1" -- "0..*" ${destino} : ${c.nombre}`);
    } else if (nombresEnums.includes(destino)) {
      clases.push(`    ${e.nombre} --> ${destino}`);
    }
  }
}
clases.push("```");

// ---------- Diagrama entidad-relación ----------
const er = ["```mermaid", "erDiagram"];
for (const e of entidades) {
  for (const c of e.campos) {
    const rel = relacion(c);
    const destino = tipoBase(c.tipo);
    if (!nombresEntidades.includes(destino)) continue;
    if (rel === "ManyToMany") {
      er.push(`    ${sinEntity(e.nombre).toUpperCase()} }o--o{ ${sinEntity(destino).toUpperCase()} : ${c.nombre}`);
    } else if (rel === "ManyToOne") {
      er.push(`    ${sinEntity(destino).toUpperCase()} ||--o{ ${sinEntity(e.nombre).toUpperCase()} : ${c.nombre}`);
    } else if (rel === "OneToOne") {
      er.push(`    ${sinEntity(destino).toUpperCase()} ||--o| ${sinEntity(e.nombre).toUpperCase()} : ${c.nombre}`);
    }
  }
}
er.push("");
for (const e of entidades) {
  er.push(`    ${sinEntity(e.nombre).toUpperCase()} {`);
  for (const c of e.campos) {
    const rel = relacion(c);
    const destino = tipoBase(c.tipo);
    if (rel === "ManyToMany" || rel === "OneToMany" || rel === "Inversa") continue;
    if (rel === "ManyToOne" || rel === "OneToOne") {
      er.push(`        Long ${nombreColumna(c)} FK`);
      continue;
    }
    const tipo = nombresEnums.includes(destino) ? "String" : c.tipo;
    const pk = c.anotaciones.some((a) => a.startsWith("@Id")) ? " PK" : "";
    er.push(`        ${tipo} ${c.nombre}${pk}`);
  }
  er.push("    }");
}
er.push("```");

// ---------- Modelo de Chen (entidades = rectángulos, relaciones = rombos, atributos = óvalos) ----------
// Mermaid no tiene notación de Chen propia, así que se imita con un diagrama de flujo.
// Nombre de cada relación, en lenguaje del dominio: "Entidad.campo": nombre
const nombresRelacion = {
  "UsuarioEntity.roles": "tiene",
  "PerfilCuidadorEntity.usuario": "ofrece",
  "SolicitudEntity.familia": "crea",
  "SolicitudEntity.cuidador": "recibe",
  "ValoracionEntity.solicitud": "genera",
  "PagoEntity.solicitud": "se paga con",
};
// GitHub dibuja los diagramas de flujo grandes con otro motor (elk) y puede fallar: se fuerza dagre.
const chen = ["```mermaid", "---", "config:", "  layout: dagre", "---", "flowchart TB"];
for (const e of entidades) {
  const idE = "e_" + sinEntity(e.nombre);
  chen.push(`    ${idE}["${sinEntity(e.nombre).toUpperCase()}"]`);
  for (const c of e.campos) {
    const rel = relacion(c);
    if (rel) continue;
    const idA = `a_${sinEntity(e.nombre)}_${c.nombre}`;
    chen.push(`    ${idA}(["${c.nombre}"]) --- ${idE}`);
  }
}
chen.push("");
for (const e of entidades) {
  for (const c of e.campos) {
    const rel = relacion(c);
    const destino = tipoBase(c.tipo);
    if (!["ManyToOne", "OneToOne", "ManyToMany"].includes(rel)) continue;
    const idR = `r_${sinEntity(e.nombre)}_${c.nombre}`;
    const nombre = nombresRelacion[`${e.nombre}.${c.nombre}`] || c.nombre;
    const lado = rel === "ManyToMany" ? ["M", "N"] : rel === "ManyToOne" ? ["1", "N"] : ["1", "0..1"];
    chen.push(`    ${idR}{"${nombre}"}`);
    chen.push(`    e_${sinEntity(destino)} ---|"${lado[0]}"| ${idR}`);
    chen.push(`    ${idR} ---|"${lado[1]}"| e_${sinEntity(e.nombre)}`);
  }
}
chen.push("```");

console.log("## Diagrama de clases\n");
console.log(clases.join("\n"));
console.log("\n## Diagrama entidad-relación\n");
console.log(er.join("\n"));
console.log("\n## Modelo de Chen\n");
console.log(chen.join("\n"));