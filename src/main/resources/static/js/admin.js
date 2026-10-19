/* Admin dashboard charts. Data comes from data-labels / data-values attributes rendered by the server. */
(function () {
    "use strict";
    if (typeof Chart === "undefined") return;

    const css = getComputedStyle(document.documentElement);
    const ink = css.getPropertyValue("--ink-soft").trim() || "#46524b";
    Chart.defaults.font.family = css.getPropertyValue("--font-body").trim() || "system-ui";
    Chart.defaults.color = ink;

    const crowdColours = {
        LOW: "#5c9a6b",
        MODERATE: "#d9a521",
        HIGH: "#c4663f",
        VERY_HIGH: "#a63a3a"
    };
    const read = (el, name) => (el.dataset[name] || "").split("|").filter((x) => x.length);

    const crowd = document.getElementById("crowdChart");
    if (crowd) {
        const labels = read(crowd, "labels");
        const values = read(crowd, "values").map(Number);
        new Chart(crowd, {
            type: "doughnut",
            data: {
                labels: labels.map((l) => l.replace("_", " ").toLowerCase()),
                datasets: [{data: values, backgroundColor: labels.map((l) => crowdColours[l] || "#999"), borderColor: "#fffcf6", borderWidth: 4, hoverOffset: 6}]
            },
            options: {cutout: "62%", plugins: {legend: {position: "bottom", labels: {usePointStyle: true, padding: 16}}}, maintainAspectRatio: false}
        });
    }

    const top = document.getElementById("topStationsChart");
    if (top) {
        new Chart(top, {
            type: "bar",
            data: {
                labels: read(top, "labels"),
                datasets: [{data: read(top, "values").map(Number), backgroundColor: "#2f5d50", borderRadius: 10, maxBarThickness: 26}]
            },
            options: {
                indexAxis: "y", maintainAspectRatio: false,
                plugins: {legend: {display: false}},
                scales: {
                    x: {beginAtZero: true, ticks: {precision: 0}, grid: {color: "rgba(0,0,0,.05)"}},
                    y: {grid: {display: false}}
                }
            }
        });
    }
})();
