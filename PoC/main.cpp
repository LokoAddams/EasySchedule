#include <iostream>
#include <vector>
#include <map>
#include <queue>
#include <set>
#include <algorithm>
#include <string>

using namespace std;

// --- ESTRUCTURAS DE DATOS ---
struct Horario {
    int dia;
    int inicio;
    int fin;
};

struct Paralelo {
    int id;
    vector<Horario> horarios;
};

struct Materia {
    int id;
    vector<Paralelo> paralelos;
};

struct Preferencias {
    int peso_manana_tarde;
    bool prefiere_manana; 
    int peso_puentes_cortos;
    bool prefiere_puentes_cortos;
    int peso_max_por_dia; 
    int max_materias_por_dia;
    int peso_paralelos_pref;
    map<int, int> paralelos_preferidos;
    int cant_materias_tomar;
    map<int, int> materia_paralelo_obligatorio; 
};

struct ResultadoHorario {
    vector<pair<Materia, Paralelo>> seleccion;
    int puntaje;

    bool operator<(const ResultadoHorario& otro) const {
        return puntaje > otro.puntaje;
    }
};

// --- VARIABLES GLOBALES ---
map<int, vector<int>> grafo_prerrequisitos; 
map<int, int> grados_entrada_global; 

// --- FUNCIONES AUXILIARES ---
bool hayCruce(const vector<Horario>& h1, const vector<Horario>& h2) {
    for (const auto& a : h1) {
        for (const auto& b : h2) {
            if (a.dia == b.dia) {
                if (a.inicio < b.fin && b.inicio < a.fin) return true;
            }
        }
    }
    return false;
}

int evaluarHorario(const vector<pair<Materia, Paralelo>>& seleccion, const Preferencias& pref) {
    int puntaje = 0;
    map<int, int> materias_por_dia;
    map<int, vector<pair<int, int>>> horas_por_dia;

    for (const auto& item : seleccion) {
        int mat_id = item.first.id;
        int par_id = item.second.id;

        if (pref.paralelos_preferidos.count(mat_id) && pref.paralelos_preferidos.at(mat_id) == par_id) {
            puntaje += pref.peso_paralelos_pref * 15;
        }

        for (const auto& h : item.second.horarios) {
            materias_por_dia[h.dia]++;
            horas_por_dia[h.dia].push_back({h.inicio, h.fin});
            
            bool es_manana = h.inicio < 13;
            if (es_manana == pref.prefiere_manana) puntaje += pref.peso_manana_tarde * 10; 
        }
    }

    for (auto const& [dia, count] : materias_por_dia) {
        if (count > pref.max_materias_por_dia) puntaje -= pref.peso_max_por_dia * 50; 
        else puntaje += pref.peso_max_por_dia * 10;
    }

    for (auto& [dia, horas] : horas_por_dia) {
        if (horas.size() > 1) {
            sort(horas.begin(), horas.end());
            for (size_t i = 0; i < horas.size() - 1; i++) {
                int puente = horas[i+1].first - horas[i].second;
                if (pref.prefiere_puentes_cortos) {
                    if (puente == 0) puntaje += pref.peso_puentes_cortos * 20;
                    else if (puente <= 2) puntaje += pref.peso_puentes_cortos * 10;
                    else puntaje -= pref.peso_puentes_cortos * 5; 
                }
            }
        }
    }
    return puntaje;
}

vector<vector<int>> proyectarMaterias(const vector<int>& materias_aprobadas_ahora) {
    vector<vector<int>> niveles;
    map<int, int> grados = grados_entrada_global;
    queue<int> q;

    for (int m : materias_aprobadas_ahora) {
        for (int v : grafo_prerrequisitos[m]) {
            grados[v]--;
            if (grados[v] == 0) q.push(v);
        }
    }

    while (!q.empty()) {
        int size = q.size();
        vector<int> nivel_actual;
        for (int i = 0; i < size; i++) {
            int u = q.front();
            q.pop();
            nivel_actual.push_back(u);
            
            for (int v : grafo_prerrequisitos[u]) {
                grados[v]--;
                if (grados[v] == 0) q.push(v);
            }
        }
        if (!nivel_actual.empty()) niveles.push_back(nivel_actual);
    }
    return niveles;
}

void generarHorarios(int idx_materia, vector<Materia>& materias_disponibles, 
                     vector<pair<Materia, Paralelo>>& seleccion_actual, 
                     Preferencias& pref, vector<ResultadoHorario>& resultados) {
    
    if (seleccion_actual.size() == pref.cant_materias_tomar) {
        ResultadoHorario res;
        res.seleccion = seleccion_actual; 
        res.puntaje = evaluarHorario(seleccion_actual, pref);
        resultados.push_back(res);
        return;
    }

    if (idx_materia >= materias_disponibles.size()) return;

    Materia& mat_actual = materias_disponibles[idx_materia];
    generarHorarios(idx_materia + 1, materias_disponibles, seleccion_actual, pref, resultados);

    for (Paralelo& p : mat_actual.paralelos) {
        if (pref.materia_paralelo_obligatorio.count(mat_actual.id) && 
            pref.materia_paralelo_obligatorio[mat_actual.id] != p.id) {
            continue;
        }

        bool cruce = false;
        for (auto& sel : seleccion_actual) {
            if (hayCruce(sel.second.horarios, p.horarios)) {
                cruce = true; break;
            }
        }

        if (!cruce) {
            seleccion_actual.push_back({mat_actual, p});
            generarHorarios(idx_materia + 1, materias_disponibles, seleccion_actual, pref, resultados);
            seleccion_actual.pop_back(); 
        }
    }
}

int main() {
    Materia m1 = {101, {{1, {{0, 8, 10}, {2, 8, 10}}}, {2, {{0, 14, 16}, {2, 14, 16}}}}};
    Materia m2 = {102, {{1, {{1, 10, 12}, {3, 10, 12}}}, {2, {{1, 16, 18}, {3, 16, 18}}}}}; 
    Materia m3 = {103, {{1, {{0, 10, 12}, {2, 10, 12}}}}}; 
    Materia m4 = {104, {{1, {{1, 8, 10}, {3, 8, 10}}}}};   
    
    vector<Materia> disponibles = {m1, m2, m3, m4};

    grafo_prerrequisitos[101] = {201, 202};
    grafo_prerrequisitos[102] = {203};
    grafo_prerrequisitos[201] = {301}; 
    
    grados_entrada_global[201] = 1;
    grados_entrada_global[202] = 1;
    grados_entrada_global[203] = 1;
    grados_entrada_global[301] = 1;

    Preferencias pref;
    pref.cant_materias_tomar = 3; 
    pref.materia_paralelo_obligatorio[103] = 1; 
    pref.peso_manana_tarde = 4; 
    pref.prefiere_manana = true;
    pref.peso_puentes_cortos = 3;
    pref.prefiere_puentes_cortos = true;
    pref.peso_max_por_dia = 2;
    pref.max_materias_por_dia = 2;
    pref.peso_paralelos_pref = 1; 
    pref.paralelos_preferidos[101] = 2; 

    vector<ResultadoHorario> resultados;
    vector<pair<Materia, Paralelo>> seleccion_actual;
    
    generarHorarios(0, disponibles, seleccion_actual, pref, resultados);

    sort(resultados.begin(), resultados.end());
    string nombres_dias[] = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes"};

    cout << "--- TOP HORARIOS GENERADOS ---\n\n";
    int top = min((int)resultados.size(), 10);
    
    for (int i = 0; i < top; i++) {
        cout << "Opcion #" << i + 1 << " (Puntaje: " << resultados[i].puntaje << ")\n";
        
        map<int, vector<string>> horario_por_dia;
        for (auto& par : resultados[i].seleccion) {
            for (auto& h : par.second.horarios) {
                string texto_clase = "Materia " + to_string(par.first.id) + 
                                     " - P" + to_string(par.second.id) + 
                                     " (" + to_string(h.inicio) + ":00 a " + to_string(h.fin) + ":00)";
                horario_por_dia[h.dia].push_back(texto_clase);
            }
        }

        for (int d = 0; d < 5; d++) {
            if (horario_por_dia.count(d)) {
                sort(horario_por_dia[d].begin(), horario_por_dia[d].end()); 
                cout << "  " << nombres_dias[d] << ":\n";
                for (const string& clase : horario_por_dia[d]) {
                    cout << "    - " << clase << "\n";
                }
            }
        }

        if (i == 0) {
            cout << "\n  [PROYECCION] Materias que desbloqueas a futuro:\n";
            vector<int> materias_aprobadas;
            for (auto& p : resultados[i].seleccion) materias_aprobadas.push_back(p.first.id);
            
            vector<vector<int>> proyeccion = proyectarMaterias(materias_aprobadas);
            if (proyeccion.empty()) {
                cout << "    No desbloqueas ninguna materia nueva.\n";
            } else {
                for (size_t nivel = 0; nivel < proyeccion.size(); nivel++) {
                    cout << "    > Semestre +" << nivel + 1 << ": ";
                    for (int mat_abierta : proyeccion[nivel]) cout << mat_abierta << " ";
                    cout << "\n";
                }
            }
        }
        cout << "-----------------------------------\n";
    }

    if (resultados.empty()) cout << "No se encontro ningun horario valido.\n";
    return 0;
}